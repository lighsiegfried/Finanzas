package com.kratt.finanzas.presentation.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.repository.TemplateSaveResult
import com.kratt.finanzas.data.repository.TransactionTemplateRepository
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionTemplate
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.TemplateValidationError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// estado del formulario de plantilla; el monto queda como texto hasta guardar
data class TemplateFormUiState(
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val amountText: String = "",
    val description: String = "",
    val errors: Set<TemplateValidationError> = emptySet(),
    val isSaved: Boolean = false,
)

// crea o edita una plantilla reutilizable de movimiento
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateFormViewModel(
    private val repo: TransactionTemplateRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val templateId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateFormUiState())
    val uiState: StateFlow<TemplateFormUiState> = _uiState.asStateFlow()

    // saber si es edicion sirve para el titulo de la pantalla
    val isEditing: Boolean = templateId != null

    val accounts: StateFlow<List<Account>> = accountRepository.observeActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // las categorias dependen del tipo elegido en el formulario
    val categories: StateFlow<List<Category>> = _uiState
        .map { it.type }
        .flatMapLatest { type -> categoryRepository.observeActiveByType(type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // si viene un id cargamos la plantilla para editarla
        if (templateId != null) {
            viewModelScope.launch {
                repo.findById(templateId)?.let { template ->
                    _uiState.update {
                        it.copy(
                            name = template.name,
                            type = template.type,
                            accountId = template.accountId,
                            destinationAccountId = template.destinationAccountId,
                            categoryId = template.categoryId,
                            amountText = template.defaultAmountCents?.let { cents ->
                                AmountParser.formatCents(cents)
                            } ?: "",
                            description = template.description.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value, errors = it.errors - TemplateValidationError.NAME_REQUIRED - TemplateValidationError.DUPLICATE_NAME)
    }

    // al cambiar el tipo limpiamos categoria o destino que ya no aplican
    fun onTypeChange(type: TransactionType) = _uiState.update {
        it.copy(
            type = type,
            categoryId = if (type == TransactionType.TRANSFER) null else it.categoryId,
            destinationAccountId = if (type == TransactionType.TRANSFER) it.destinationAccountId else null,
            errors = emptySet(),
        )
    }

    fun onAccountSelected(id: Long) = _uiState.update {
        it.copy(accountId = id, errors = it.errors - TemplateValidationError.ACCOUNT_REQUIRED - TemplateValidationError.SAME_ACCOUNTS)
    }

    fun onDestinationSelected(id: Long) = _uiState.update {
        it.copy(destinationAccountId = id, errors = it.errors - TemplateValidationError.ACCOUNT_REQUIRED - TemplateValidationError.SAME_ACCOUNTS)
    }

    fun onCategorySelected(id: Long) = _uiState.update {
        it.copy(categoryId = id, errors = it.errors - TemplateValidationError.CATEGORY_REQUIRED)
    }

    // solo aceptamos lo que parece un monto valido mientras se escribe
    fun onAmountChange(value: String) {
        if (value.isEmpty() || AmountParser.isPartialInput(value)) {
            _uiState.update { it.copy(amountText = value, errors = it.errors - TemplateValidationError.INVALID_AMOUNT) }
        }
    }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    // arma la plantilla desde el estado y la manda a guardar
    fun onSave() {
        val state = _uiState.value
        val amountCents = if (state.amountText.isBlank()) null else AmountParser.parseToCents(state.amountText)
        // si el texto no esta vacio pero no se pudo leer, marcamos monto invalido de una vez
        if (state.amountText.isNotBlank() && amountCents == null) {
            _uiState.update { it.copy(errors = it.errors + TemplateValidationError.INVALID_AMOUNT) }
            return
        }
        val template = TransactionTemplate(
            id = templateId ?: 0L,
            name = state.name,
            type = state.type,
            accountId = state.accountId ?: 0L,
            destinationAccountId = if (state.type == TransactionType.TRANSFER) state.destinationAccountId else null,
            categoryId = if (state.type == TransactionType.TRANSFER) null else state.categoryId,
            defaultAmountCents = amountCents,
            description = state.description.ifBlank { null },
        )
        viewModelScope.launch {
            when (val result = repo.save(template)) {
                is TemplateSaveResult.Success -> _uiState.update { it.copy(isSaved = true, errors = emptySet()) }
                is TemplateSaveResult.Invalid -> _uiState.update { it.copy(errors = result.errors) }
            }
        }
    }
}
