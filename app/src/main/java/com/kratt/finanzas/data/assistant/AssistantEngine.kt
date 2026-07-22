package com.kratt.finanzas.data.assistant

import com.kratt.finanzas.domain.assistant.AnswerFactory
import com.kratt.finanzas.domain.assistant.AssistantAnswer
import com.kratt.finanzas.domain.assistant.AssistantMode
import com.kratt.finanzas.domain.assistant.AssistantModeSelector
import com.kratt.finanzas.domain.assistant.AssistantPlan
import com.kratt.finanzas.domain.assistant.AssistantPlanner
import com.kratt.finanzas.domain.assistant.AssistantStatus
import com.kratt.finanzas.domain.assistant.GenerativeAvailability
import com.kratt.finanzas.domain.assistant.ParsedQuery
import com.kratt.finanzas.domain.assistant.QueryParser
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

// orquesta el asistente: interpreta, planifica, ejecuta una herramienta y arma la respuesta
// el modo determinista siempre funciona; el modo avanzado solo mejora la interpretacion y cae seguro
class AssistantEngine(
    private val executor: AssistantToolExecutor,
    private val generative: GenerativeAssistant = NoOpGenerativeAssistant(),
    private val deviceSupported: () -> Boolean,
    private val userGenerativeEnabled: () -> Boolean = { false },
    private val today: () -> LocalDate = LocalDate::now,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {

    // evita generaciones en paralelo del modo avanzado
    private val generationMutex = Mutex()

    // estado que la ui muestra sobre el modo activo y la disponibilidad del modo avanzado
    fun status(): AssistantStatus {
        val supported = deviceSupported() && generative.isRuntimeReady()
        val enabled = userGenerativeEnabled()
        return AssistantStatus(
            activeMode = AssistantModeSelector.select(deviceSupported(), enabled, generative.isRuntimeReady()),
            generativeAvailability = when {
                !supported -> GenerativeAvailability.UNSUPPORTED_DEVICE
                !enabled -> GenerativeAvailability.DISABLED_BY_USER
                else -> GenerativeAvailability.AVAILABLE
            },
        )
    }

    // responde una pregunta; cualquier fallo cae a un mensaje generico sin detalles tecnicos
    suspend fun answer(query: String): AssistantAnswer {
        if (query.isBlank()) return AssistantAnswer.UnsupportedAnswer
        return try {
            val parsed = interpret(query)
            val context = executor.loadContext()
            when (val plan = AssistantPlanner.plan(parsed, context, today())) {
                is AssistantPlan.Run -> AnswerFactory.build(plan, executor.execute(plan.request), today())
                is AssistantPlan.Clarify -> AssistantAnswer.ClarificationAnswer(plan.kind, plan.options)
                is AssistantPlan.Draft -> AssistantAnswer.DraftAnswer(plan.draft)
                AssistantPlan.Help -> AssistantAnswer.HelpAnswer
                AssistantPlan.Unsupported -> AssistantAnswer.UnsupportedAnswer
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AssistantAnswer.ErrorAnswer
        }
    }

    // interpreta la pregunta; en modo avanzado intenta mejorar la intencion y cae a lo determinista
    private suspend fun interpret(query: String): ParsedQuery {
        val deterministic = QueryParser.parse(query)
        val mode = AssistantModeSelector.select(deviceSupported(), userGenerativeEnabled(), generative.isRuntimeReady())
        if (mode == AssistantMode.DETERMINISTIC_LOCAL) return deterministic
        // nunca deja que el modelo cambie un borrador ni los calculos; solo la intencion de lectura
        if (deterministic.draft != null) return deterministic
        return try {
            generationMutex.withLock {
                withTimeout(timeoutMillis) {
                    val intent = generative.proposeIntent(query)
                    if (intent != null) deterministic.copy(intent = intent) else deterministic
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            // tiempo agotado: se usa el modo compatible determinista
            deterministic
        } catch (cancelled: CancellationException) {
            // cancelacion real del usuario, se propaga
            throw cancelled
        } catch (error: Exception) {
            // fallo del runtime: se usa el modo compatible determinista
            deterministic
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 4_000L
    }
}
