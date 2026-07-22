package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.usecase.DateRange
import com.kratt.finanzas.domain.usecase.TextNormalizer
import java.time.LocalDate
import java.time.YearMonth

// interpreta el periodo pedido en espanol de forma determinista, sin analizar texto libre de fechas
object PeriodResolver {

    // nombres de mes normalizados sin acentos, con las dos formas de septiembre
    private val months: Map<String, Int> = mapOf(
        "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4, "mayo" to 5, "junio" to 6,
        "julio" to 7, "agosto" to 8, "septiembre" to 9, "setiembre" to 9, "octubre" to 10,
        "noviembre" to 11, "diciembre" to 12,
    )

    private val numberWords: Map<String, Int> = mapOf(
        "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
    )

    // acepta anos 19xx y 20xx; un ano fuera del historial da un periodo vacio, no un cambio silencioso
    private val yearRegex = Regex("\\b((?:19|20)\\d{2})\\b")

    // periodo por defecto: el mes actual, marcado como no explicito
    fun defaultMonth(today: LocalDate): ResolvedPeriod {
        val month = YearMonth.from(today)
        return monthPeriod(month, wasExplicit = false)
    }

    fun resolve(rawQuery: String, today: LocalDate): PeriodResolution {
        val text = TextNormalizer.normalize(rawQuery)
        val currentMonth = YearMonth.from(today)
        val year = yearRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

        // ultimos N meses, incluye el mes actual
        if (text.contains("ultimos") || text.contains("ultimas")) {
            if (text.contains("mes")) {
                val n = extractCount(text) ?: 3
                val clamped = n.coerceIn(2, 12)
                val start = currentMonth.minusMonths((clamped - 1).toLong()).atDay(1)
                val end = currentMonth.atEndOfMonth()
                return PeriodResolution.Resolved(
                    ResolvedPeriod(DateRange(start, end), PeriodLabel.LastMonths(clamped), wasExplicit = true),
                )
            }
        }

        // mes anterior
        if (text.contains("mes pasado") || text.contains("mes anterior") || text.contains("ultimo mes")) {
            return PeriodResolution.Resolved(monthPeriod(currentMonth.minusMonths(1), wasExplicit = true))
        }

        // un mes por su nombre, con o sin ano; sin ano se asume el actual y se muestra
        val namedMonth = months.entries.firstOrNull { containsWord(text, it.key) }
        if (namedMonth != null) {
            val resolvedYear = year ?: currentMonth.year
            return PeriodResolution.Resolved(monthPeriod(YearMonth.of(resolvedYear, namedMonth.value), wasExplicit = true))
        }

        // el ano actual o un ano indicado
        if (text.contains("este ano") || text.contains("el ano") || text.contains("anio")) {
            val y = year ?: today.year
            return PeriodResolution.Resolved(yearPeriod(y))
        }

        // menciona "mes de" sin nombre de mes: pedir aclaracion
        // "del mes" solo (por ejemplo "resumen del mes") significa el mes actual, no es ambiguo
        if (text.contains("mes de") && !text.contains("este mes") && !text.contains("mes actual")) {
            return PeriodResolution.AmbiguousMonth
        }

        // este mes de forma explicita
        if (text.contains("este mes") || text.contains("mes actual")) {
            return PeriodResolution.Resolved(monthPeriod(currentMonth, wasExplicit = true))
        }

        // un ano suelto sin otra referencia
        if (year != null && !text.contains("mes")) {
            return PeriodResolution.Resolved(yearPeriod(year))
        }

        // sin referencia clara: mes actual por defecto
        return PeriodResolution.Resolved(defaultMonth(today))
    }

    // arma el periodo previo para una comparacion, un mes antes del periodo actual
    fun previousMonthOf(period: ResolvedPeriod): ResolvedPeriod {
        val label = period.label
        val month = when (label) {
            is PeriodLabel.Month -> YearMonth.of(label.year, label.month)
            else -> YearMonth.from(period.range.start)
        }
        return monthPeriod(month.minusMonths(1), wasExplicit = true)
    }

    private fun monthPeriod(month: YearMonth, wasExplicit: Boolean): ResolvedPeriod =
        ResolvedPeriod(
            range = DateRange(month.atDay(1), month.atEndOfMonth()),
            label = PeriodLabel.Month(month.year, month.monthValue),
            wasExplicit = wasExplicit,
        )

    private fun yearPeriod(year: Int): ResolvedPeriod =
        ResolvedPeriod(
            range = DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)),
            label = PeriodLabel.Year(year),
            wasExplicit = true,
        )

    // busca un numero en digitos o en palabra dentro del texto
    private fun extractCount(text: String): Int? {
        Regex("\\b(\\d{1,2})\\b").find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return numberWords.entries.firstOrNull { containsWord(text, it.key) }?.value
    }

    // coincide la palabra completa para no confundir "un" dentro de otra palabra
    private fun containsWord(text: String, word: String): Boolean =
        Regex("(^|\\s)${Regex.escape(word)}(\\s|$)").containsMatchIn(text)
}
