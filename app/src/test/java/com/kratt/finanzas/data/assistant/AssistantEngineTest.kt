package com.kratt.finanzas.data.assistant

import com.kratt.finanzas.domain.assistant.AssistantAnswer
import com.kratt.finanzas.domain.assistant.AssistantIntent
import com.kratt.finanzas.domain.assistant.PlanContextData
import com.kratt.finanzas.domain.assistant.ToolRequest
import com.kratt.finanzas.domain.assistant.ToolResult
import com.kratt.finanzas.domain.model.MonthlySummary
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantEngineTest {

    private val today = { LocalDate.of(2026, 7, 15) }

    // ejecutor falso que devuelve un resumen o lanza un error segun se configure
    private class FakeExecutor(
        private val throwOnExecute: Boolean = false,
    ) : AssistantToolExecutor {
        override suspend fun execute(request: ToolRequest): ToolResult {
            if (throwOnExecute) throw RuntimeException("db down")
            return when (request) {
                is ToolRequest.MonthlySummary -> ToolResult.MonthlySummaryResult(MonthlySummary(1000, 500, 500))
                else -> ToolResult.MonthlySummaryResult(MonthlySummary(0, 0, 0))
            }
        }

        override suspend fun loadContext(): PlanContextData = PlanContextData()
    }

    // modo avanzado falso: puede estar listo, lanzar o tardar mas del tiempo limite
    private class FakeGenerative(
        private val ready: Boolean,
        private val behavior: suspend () -> AssistantIntent? = { null },
    ) : GenerativeAssistant {
        override fun isRuntimeReady(): Boolean = ready
        override suspend fun proposeIntent(query: String): AssistantIntent? = behavior()
    }

    private fun engine(
        executor: AssistantToolExecutor = FakeExecutor(),
        generative: GenerativeAssistant = NoOpGenerativeAssistant(),
        deviceSupported: Boolean = false,
        userEnabled: Boolean = false,
    ) = AssistantEngine(
        executor = executor,
        generative = generative,
        deviceSupported = { deviceSupported },
        userGenerativeEnabled = { userEnabled },
        today = today,
        timeoutMillis = 1_000,
    )

    @Test
    fun deterministic_answer_for_summary_query() = runTest {
        val answer = engine().answer("¿cuánto gané este mes?")
        assertTrue(answer is AssistantAnswer.MonthlySummaryAnswer)
    }

    @Test
    fun write_request_returns_draft() = runTest {
        val answer = engine().answer("Registra Q200 de gasolina")
        assertTrue(answer is AssistantAnswer.DraftAnswer)
    }

    @Test
    fun blank_query_is_unsupported() = runTest {
        assertEquals(AssistantAnswer.UnsupportedAnswer, engine().answer("   "))
    }

    @Test
    fun executor_error_returns_generic_error() = runTest {
        val answer = engine(executor = FakeExecutor(throwOnExecute = true)).answer("¿cuánto gané este mes?")
        assertEquals(AssistantAnswer.ErrorAnswer, answer)
    }

    @Test
    fun generative_timeout_falls_back_to_deterministic() = runTest {
        val slow = FakeGenerative(ready = true, behavior = { delay(10_000); AssistantIntent.HELP })
        val answer = engine(generative = slow, deviceSupported = true, userEnabled = true).answer("¿cuánto gané este mes?")
        // aun con el modo avanzado tardando, la respuesta determinista se entrega
        assertTrue(answer is AssistantAnswer.MonthlySummaryAnswer)
    }

    @Test
    fun generative_failure_falls_back_to_deterministic() = runTest {
        val failing = FakeGenerative(ready = true, behavior = { throw RuntimeException("runtime error") })
        val answer = engine(generative = failing, deviceSupported = true, userEnabled = true).answer("¿cuánto gané este mes?")
        assertTrue(answer is AssistantAnswer.MonthlySummaryAnswer)
    }

    @Test
    fun model_unavailable_still_answers_with_deterministic_mode() = runTest {
        // dispositivo objetivo: sin runtime, el modo compatible responde igual
        val answer = engine(generative = NoOpGenerativeAssistant(), deviceSupported = true).answer("¿cuánto gané este mes?")
        assertTrue(answer is AssistantAnswer.MonthlySummaryAnswer)
    }

    @Test
    fun status_reports_unsupported_on_target_device() {
        val status = engine(deviceSupported = false).status()
        assertEquals(com.kratt.finanzas.domain.assistant.GenerativeAvailability.UNSUPPORTED_DEVICE, status.generativeAvailability)
    }
}
