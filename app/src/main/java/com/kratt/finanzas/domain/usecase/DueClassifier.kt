package com.kratt.finanzas.domain.usecase

import java.time.LocalDate

// clasifica un compromiso por su fecha usando la fecha local, no texto
enum class DueWhen { OVERDUE, TODAY, TOMORROW, LATER }

object DueClassifier {
    fun classify(dueDate: LocalDate, today: LocalDate): DueWhen = when {
        dueDate.isBefore(today) -> DueWhen.OVERDUE
        dueDate == today -> DueWhen.TODAY
        dueDate == today.plusDays(1) -> DueWhen.TOMORROW
        else -> DueWhen.LATER
    }
}

// calcula cuando avisar de un pago segun los dias de anticipacion
object ReminderCalculator {

    fun remindOn(dueDate: LocalDate, daysBefore: Int): LocalDate = dueDate.minusDays(daysBefore.toLong())

    // el compromiso ya entra en la ventana de aviso, incluye los vencidos
    fun isDueForReminder(dueDate: LocalDate, today: LocalDate, daysBefore: Int): Boolean =
        !today.isBefore(remindOn(dueDate, daysBefore))
}
