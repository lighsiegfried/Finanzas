package com.kratt.finanzas.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.domain.model.RecurringOccurrence
import com.kratt.finanzas.domain.model.RecurringTemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringRow(
    val template: RecurringTemplate,
    val nextOccurrence: RecurringOccurrence?,
)

data class RecurringUiState(
    val isLoading: Boolean = true,
    val rows: List<RecurringRow> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringViewModel(
    private val recurringRepository: RecurringRepository,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.observeTemplates(),
        refresh,
    ) { templates, _ -> templates }
        .map { templates ->
            RecurringUiState(
                isLoading = false,
                rows = templates.map { RecurringRow(it, recurringRepository.nextPending(it.id)) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    // confirmar crea el movimiento real; el refresh vuelve a leer la proxima ocurrencia
    fun onConfirm(occurrenceId: Long) {
        viewModelScope.launch {
            recurringRepository.postOccurrence(occurrenceId)
            refresh.value++
        }
    }

    fun onSkip(occurrenceId: Long) {
        viewModelScope.launch {
            recurringRepository.skipOccurrence(occurrenceId)
            refresh.value++
        }
    }
}
