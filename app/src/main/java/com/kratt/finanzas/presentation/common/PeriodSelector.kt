package com.kratt.finanzas.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.usecase.ReportPeriod

// etiqueta en espanol de cada periodo
@StringRes
fun ReportPeriod.labelRes(): Int = when (this) {
    ReportPeriod.THIS_MONTH -> R.string.period_this_month
    ReportPeriod.LAST_MONTH -> R.string.period_last_month
    ReportPeriod.LAST_3_MONTHS -> R.string.period_last_3
    ReportPeriod.LAST_6_MONTHS -> R.string.period_last_6
    ReportPeriod.THIS_YEAR -> R.string.period_this_year
    ReportPeriod.CUSTOM -> R.string.period_custom
}

// selector de periodo reutilizable con chips; usa los periodos deterministas existentes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(
    selected: ReportPeriod,
    onSelected: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag(TestTags.PERIOD_SELECTOR),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportPeriod.entries.forEach { period ->
                FilterChip(
                    selected = selected == period,
                    onClick = { onSelected(period) },
                    label = { Text(stringResource(period.labelRes())) },
                    modifier = Modifier.testTag("${TestTags.PERIOD_CHIP}_${period.name}"),
                )
            }
        }
    }
}
