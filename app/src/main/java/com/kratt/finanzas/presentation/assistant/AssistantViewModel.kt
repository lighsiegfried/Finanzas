package com.kratt.finanzas.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.assistant.AssistantEngine
import com.kratt.finanzas.domain.assistant.AssistantAnswer
import com.kratt.finanzas.domain.assistant.AssistantStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// un mensaje de la conversacion, solo en memoria
sealed interface AssistantChatMessage {
    data class User(val text: String) : AssistantChatMessage
    data class Bot(val answer: AssistantAnswer) : AssistantChatMessage
}

data class AssistantUiState(
    val messages: List<AssistantChatMessage> = emptyList(),
    val input: String = "",
    val isAnalyzing: Boolean = false,
    val status: AssistantStatus,
)

// mantiene la conversacion en memoria; nunca la guarda, ni la registra en logs, ni la respalda
class AssistantViewModel(private val engine: AssistantEngine) : ViewModel() {

    private val _state = MutableStateFlow(AssistantUiState(status = engine.status()))
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    private var answerJob: Job? = null

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text) }
    }

    // envia una pregunta; evita envios duplicados mientras se analiza
    fun onSend(rawText: String = _state.value.input) {
        val query = rawText.trim()
        if (query.isEmpty() || _state.value.isAnalyzing) return
        _state.update {
            it.copy(
                messages = it.messages + AssistantChatMessage.User(query),
                input = "",
                isAnalyzing = true,
            )
        }
        // el trabajo pesado corre fuera del hilo principal y se puede cancelar
        answerJob = viewModelScope.launch(Dispatchers.Default) {
            val answer = engine.answer(query)
            _state.update {
                it.copy(messages = it.messages + AssistantChatMessage.Bot(answer), isAnalyzing = false)
            }
        }
    }

    // cancela la consulta en curso y vuelve al estado listo
    fun onCancel() {
        answerJob?.cancel()
        answerJob = null
        _state.update { it.copy(isAnalyzing = false) }
    }

    // limpia toda la conversacion de la memoria
    fun onClear() {
        answerJob?.cancel()
        answerJob = null
        _state.update { it.copy(messages = emptyList(), isAnalyzing = false) }
    }
}
