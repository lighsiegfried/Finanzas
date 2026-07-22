package com.kratt.finanzas.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountParserTest {

    @Test
    fun parse_integerAmount_convertsToCents() {
        assertEquals(12500L, AmountParser.parseToCents("125"))
    }

    @Test
    fun parse_twoDecimalAmount_convertsToCents() {
        assertEquals(12575L, AmountParser.parseToCents("125.75"))
    }

    @Test
    fun parse_oneDecimalAmount_padsToTwoDecimals() {
        assertEquals(50L, AmountParser.parseToCents("0.5"))
    }

    @Test
    fun parse_commaAsDecimalSeparator_isAccepted() {
        assertEquals(12575L, AmountParser.parseToCents("125,75"))
    }

    @Test
    fun parse_trailingSeparator_countsAsWholeAmount() {
        assertEquals(12500L, AmountParser.parseToCents("125."))
    }

    @Test
    fun parse_surroundingSpaces_areTrimmed() {
        assertEquals(12575L, AmountParser.parseToCents(" 125.75 "))
    }

    @Test
    fun parse_zeroAmount_isRejected() {
        assertNull(AmountParser.parseToCents("0"))
        assertNull(AmountParser.parseToCents("0.00"))
    }

    @Test
    fun parse_negativeAmount_isRejected() {
        assertNull(AmountParser.parseToCents("-5"))
        assertNull(AmountParser.parseToCents("-125.75"))
    }

    @Test
    fun parse_moreThanTwoDecimals_isRejected() {
        assertNull(AmountParser.parseToCents("12.345"))
    }

    @Test
    fun parse_malformedText_isRejected() {
        assertNull(AmountParser.parseToCents(""))
        assertNull(AmountParser.parseToCents("abc"))
        assertNull(AmountParser.parseToCents("12.34.5"))
        assertNull(AmountParser.parseToCents("1e3"))
        assertNull(AmountParser.parseToCents("."))
    }

    @Test
    fun parse_largeAmount_staysWithinLongRange() {
        assertEquals(999_999_999_999L, AmountParser.parseToCents("9999999999.99"))
    }

    @Test
    fun partialInput_acceptsTypingStates_andRejectsLetters() {
        assertTrue(AmountParser.isPartialInput(""))
        assertTrue(AmountParser.isPartialInput("125"))
        assertTrue(AmountParser.isPartialInput("125."))
        assertTrue(AmountParser.isPartialInput("125.7"))
        assertFalse(AmountParser.isPartialInput("12a"))
        assertFalse(AmountParser.isPartialInput("1.234"))
    }
}
