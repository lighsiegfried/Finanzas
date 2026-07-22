package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.usecase.TextNormalizer
import java.time.temporal.ChronoUnit

// heuristica local para senalar movimientos que parecen repetidos; nunca borra nada
object DuplicateDetector {

    // agrupa por tipo, cuenta, monto y descripcion, luego busca fechas cercanas
    fun detect(items: List<TransactionListItem>, windowDays: Int = 3): List<DuplicateGroup> {
        val result = mutableListOf<DuplicateGroup>()
        val groups = items.groupBy { key(it) }
        for ((_, groupItems) in groups) {
            if (groupItems.size < 2) continue
            val sorted = groupItems.sortedBy { it.date }
            var cluster = mutableListOf(sorted.first())
            fun flush() {
                if (cluster.size >= 2) {
                    val first = cluster.first()
                    result += DuplicateGroup(
                        amountCents = first.amountCents,
                        description = cluster.firstNotNullOfOrNull { it.description?.takeIf { d -> d.isNotBlank() } },
                        accountName = first.accountName,
                        dates = cluster.map { it.date },
                    )
                }
            }
            for (index in 1 until sorted.size) {
                val prev = cluster.last()
                val current = sorted[index]
                if (ChronoUnit.DAYS.between(prev.date, current.date) <= windowDays) {
                    cluster.add(current)
                } else {
                    flush()
                    cluster = mutableListOf(current)
                }
            }
            flush()
        }
        // orden estable: primero los grupos con mas repeticiones y mayor monto
        return result.sortedWith(
            compareByDescending<DuplicateGroup> { it.count }.thenByDescending { it.amountCents },
        )
    }

    private fun key(item: TransactionListItem): String {
        val desc = TextNormalizer.normalize(item.description ?: "")
        return "${item.type}|${item.accountId}|${item.amountCents}|$desc"
    }
}
