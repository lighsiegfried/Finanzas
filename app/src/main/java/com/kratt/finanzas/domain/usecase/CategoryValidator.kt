package com.kratt.finanzas.domain.usecase

// errores posibles del formulario de categoria
enum class CategoryValidationError {
    EMPTY_NAME,
    DUPLICATE_NAME,
}

object CategoryValidator {

    // valida el nombre contra las categorias activas del mismo tipo, sin la que se edita
    fun validate(
        name: String,
        existingActiveNamesLower: Set<String>,
    ): Set<CategoryValidationError> = buildSet {
        val trimmed = name.trim()
        when {
            trimmed.isEmpty() -> add(CategoryValidationError.EMPTY_NAME)
            existingActiveNamesLower.contains(trimmed.lowercase()) -> add(CategoryValidationError.DUPLICATE_NAME)
        }
    }

    // no se puede cambiar el tipo de una categoria que ya tiene movimientos
    fun canChangeType(hasMovements: Boolean): Boolean = !hasMovements
}
