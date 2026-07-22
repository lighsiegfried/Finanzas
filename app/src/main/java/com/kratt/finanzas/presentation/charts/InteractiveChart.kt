package com.kratt.finanzas.presentation.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.presentation.common.maskedAmount

// punto de la grafica; el monto real viene del dominio, nunca de las coordenadas de la grafica
data class ChartPoint(
    val label: String,
    val amountCents: Long,
) {
    // valor de visualizacion; solo se usa para dibujar, no para calcular dinero
    val displayValue: Double get() = amountCents / 100.0
}

// grafica interactiva local con seleccion accesible por chips, detalle y acceso a los movimientos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveChartCard(
    points: List<ChartPoint>,
    line: Boolean,
    summary: String,
    modifier: Modifier = Modifier,
    onViewMovements: ((Int) -> Unit)? = null,
) {
    // la privacidad de saldos oculta los montos y las etiquetas de valor de la grafica
    val balancesHidden = com.kratt.finanzas.presentation.common.LocalBalancesHidden.current
    Card(modifier = modifier.fillMaxWidth().testTag(TestTags.CHART_INTERACTIVE)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (points.isEmpty()) {
                Text(stringResource(R.string.chart_empty), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            var selected by remember(points.size) { mutableIntStateOf(-1) }
            val total = remember(points) { points.sumOf { it.amountCents } }

            val chartModifier = Modifier
                .fillMaxWidth().height(200.dp)
                // el resumen textual describe la grafica para lectores de pantalla
                .semantics { contentDescription = summary }
            if (line) {
                LineChart(points.map { it.displayValue }, chartModifier, showValueAxis = !balancesHidden)
            } else {
                ColumnChart(points.map { it.displayValue }, chartModifier, showValueAxis = !balancesHidden)
            }

            // seleccion por chips: llega sin depender de la precision del toque sobre la grafica
            Text(stringResource(R.string.chart_select_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                points.forEachIndexed { index, point ->
                    FilterChip(
                        selected = selected == index,
                        onClick = { selected = if (selected == index) -1 else index },
                        label = { Text(point.label) },
                        modifier = Modifier.testTag("${TestTags.CHART_POINT}_$index"),
                    )
                }
            }

            // el detalle aparece con una transicion breve; respeta la reduccion de animaciones del sistema
            val reduce = com.kratt.finanzas.presentation.common.reduceMotion()
            AnimatedVisibility(
                visible = selected in points.indices,
                enter = if (reduce) EnterTransition.None else fadeIn() + expandVertically(),
                exit = if (reduce) ExitTransition.None else fadeOut() + shrinkVertically(),
            ) {
                points.getOrNull(selected)?.let { point ->
                    val pct = if (total > 0) (point.amountCents * 100 / total).toInt() else 0
                    Card(modifier = Modifier.fillMaxWidth().testTag(TestTags.CHART_DETAIL_CARD)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.chart_detail_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(point.label, style = MaterialTheme.typography.titleSmall)
                            Text(maskedAmount(point.amountCents), style = MaterialTheme.typography.titleMedium)
                            // el porcentaje es solo de presentacion y se calcula con enteros, no con coordenadas
                            if (!balancesHidden && total > 0) {
                                Text(stringResource(R.string.chart_share_of_total, pct), style = MaterialTheme.typography.bodySmall)
                            }
                            if (onViewMovements != null) {
                                OutlinedButton(
                                    onClick = { onViewMovements(selected) },
                                    modifier = Modifier.fillMaxWidth().testTag(TestTags.VIEW_MOVEMENTS_BUTTON),
                                ) {
                                    Text(stringResource(R.string.view_movements))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
