package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.model.TransactionType

// filtros locales para la pantalla de movimientos
data class TransactionFilter(
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val query: String = "",
) {
    val isEmpty: Boolean
        get() = type == null && accountId == null && categoryId == null && query.isBlank()
}

object TransactionSearch {

    // aplica el filtro y la busqueda sobre la lista ya cargada del mes
    fun apply(items: List<TransactionListItem>, filter: TransactionFilter): List<TransactionListItem> {
        val normalizedQuery = TextNormalizer.normalize(filter.query)
        return items.filter { item ->
            matchesType(item, filter.type) &&
                matchesAccount(item, filter.accountId) &&
                matchesCategory(item, filter.categoryId) &&
                matchesQuery(item, normalizedQuery)
        }
    }

    private fun matchesType(item: TransactionListItem, type: TransactionType?): Boolean =
        type == null || item.type == type

    // una transferencia coincide si la cuenta es el origen o el destino
    private fun matchesAccount(item: TransactionListItem, accountId: Long?): Boolean =
        accountId == null || item.accountId == accountId || item.destinationAccountId == accountId

    private fun matchesCategory(item: TransactionListItem, categoryId: Long?): Boolean =
        categoryId == null || item.categoryId == categoryId

    private fun matchesQuery(item: TransactionListItem, normalizedQuery: String): Boolean {
        if (normalizedQuery.isEmpty()) return true
        val haystack = listOfNotNull(
            item.description,
            item.accountName,
            item.categoryName,
            item.destinationAccountName,
        )
        return haystack.any { TextNormalizer.normalize(it).contains(normalizedQuery) }
    }
}
