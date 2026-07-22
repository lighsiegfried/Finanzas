package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DueClassifierTest {

    private val today = LocalDate.of(2026, 7, 20)

    @Test
    fun classifiesByLocalDate() {
        assertEquals(DueWhen.OVERDUE, DueClassifier.classify(LocalDate.of(2026, 7, 19), today))
        assertEquals(DueWhen.TODAY, DueClassifier.classify(today, today))
        assertEquals(DueWhen.TOMORROW, DueClassifier.classify(LocalDate.of(2026, 7, 21), today))
        assertEquals(DueWhen.LATER, DueClassifier.classify(LocalDate.of(2026, 7, 30), today))
    }

    @Test
    fun reminderDateSubtractsDaysBefore() {
        assertEquals(LocalDate.of(2026, 7, 17), ReminderCalculator.remindOn(LocalDate.of(2026, 7, 20), 3))
        assertEquals(LocalDate.of(2026, 7, 20), ReminderCalculator.remindOn(LocalDate.of(2026, 7, 20), 0))
    }

    @Test
    fun isDueForReminderInsideWindowAndWhenOverdue() {
        // vence el 25, 3 dias antes = 22; hoy 20 aun no entra
        assertFalse(ReminderCalculator.isDueForReminder(LocalDate.of(2026, 7, 25), today, 3))
        // vence el 22, 3 dias antes = 19; hoy 20 ya entra
        assertTrue(ReminderCalculator.isDueForReminder(LocalDate.of(2026, 7, 22), today, 3))
        // vencido tambien avisa
        assertTrue(ReminderCalculator.isDueForReminder(LocalDate.of(2026, 7, 10), today, 3))
    }

    @Test
    fun postingGuardIsIdempotent() {
        assertTrue(RecurringPostingRules.canPost(RecurringOccurrenceStatus.PENDING))
        assertFalse(RecurringPostingRules.canPost(RecurringOccurrenceStatus.POSTED))
        assertFalse(RecurringPostingRules.canPost(RecurringOccurrenceStatus.SKIPPED))
    }
}
