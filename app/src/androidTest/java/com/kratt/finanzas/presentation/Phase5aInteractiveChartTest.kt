package com.kratt.finanzas.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.presentation.charts.ChartPoint
import com.kratt.finanzas.presentation.charts.InteractiveChartCard
import com.kratt.finanzas.presentation.common.AMOUNT_MASK
import com.kratt.finanzas.presentation.common.LocalBalancesHidden
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase5aInteractiveChartTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val points = listOf(
        ChartPoint("Comida", 30_000),
        ChartPoint("Transporte", 10_000),
    )

    @Test
    fun selectingPoint_showsDetailWithRealAmount() {
        composeRule.setContent {
            MisFinanzasTheme {
                InteractiveChartCard(points = points, line = false, summary = "resumen")
            }
        }
        // al inicio no hay tarjeta de detalle
        composeRule.onNodeWithTag(TestTags.CHART_DETAIL_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag("${TestTags.CHART_POINT}_0").performClick()
        composeRule.onNodeWithTag(TestTags.CHART_DETAIL_CARD).assertIsDisplayed()
        composeRule.onNodeWithText(CurrencyFormatter.format(30_000)).assertIsDisplayed()
    }

    @Test
    fun viewMovements_firesWithSelectedIndex() {
        var received = -1
        composeRule.setContent {
            MisFinanzasTheme {
                InteractiveChartCard(points = points, line = false, summary = "resumen", onViewMovements = { received = it })
            }
        }
        composeRule.onNodeWithTag("${TestTags.CHART_POINT}_1").performClick()
        composeRule.onNodeWithTag(TestTags.VIEW_MOVEMENTS_BUTTON).performClick()
        assertEquals(1, received)
    }

    @Test
    fun privacyOn_masksDetailAmount() {
        composeRule.setContent {
            MisFinanzasTheme {
                CompositionLocalProvider(LocalBalancesHidden provides true) {
                    InteractiveChartCard(points = points, line = false, summary = "resumen")
                }
            }
        }
        composeRule.onNodeWithTag("${TestTags.CHART_POINT}_0").performClick()
        composeRule.onNodeWithText(AMOUNT_MASK).assertIsDisplayed()
        composeRule.onNodeWithText(CurrencyFormatter.format(30_000)).assertDoesNotExist()
    }
}
