package com.rahimjon.financewidget.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Locale.US pins "1,234.56" formatting regardless of the phone's language.
private val symbols = DecimalFormatSymbols(Locale.US)

private fun decimals(digits: Int) = DecimalFormat(if (digits == 0) "#,##0" else "#,##0.${"0".repeat(digits)}", symbols)

private fun digitsFor(currency: Currency) = if (currency == Currency.KRW) 0 else 2

/** Plain number in the display currency, no symbol (used where space is tight). */
fun formatAmount(value: Double, currency: Currency): String = decimals(digitsFor(currency)).format(value)

/** Amount with its currency symbol, e.g. "$1,234.56" or "₩1,234,567"; negatives read "-$12.00". */
fun formatMoney(value: Double, currency: Currency): String {
    val symbol = if (currency == Currency.KRW) "₩" else "$"
    val body = formatAmount(kotlin.math.abs(value), currency)
    return if (value < 0 && body.any { it in '1'..'9' }) "-$symbol$body" else "$symbol$body"
}

fun formatPercent(pct: Double?): String {
    if (pct == null) return "—"
    val body = decimals(2).format(kotlin.math.abs(pct))
    return when {
        pct > 0 && body != "0.00" -> "+$body%"
        pct < 0 && body != "0.00" -> "-$body%"
        else -> "$body%"
    }
}

/** A number as plain editable text, no grouping: 150.0 -> "150", 0.25 -> "0.25". */
fun formatPlain(value: Double): String = DecimalFormat("0.########", symbols).format(value)

/** Share counts without trailing zeros: 10 -> "10", 0.5 -> "0.5", 1234.5678 -> "1,234.5678". */
fun formatShares(shares: Double): String = DecimalFormat("#,##0.########", symbols).format(shares)

/** Percent without a sign, for use beside a ▲/▼ marker. */
fun formatPercentUnsigned(pct: Double?): String =
    if (pct == null) "—" else decimals(2).format(kotlin.math.abs(pct)) + "%"
