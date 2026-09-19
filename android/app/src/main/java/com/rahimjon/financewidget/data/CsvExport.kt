package com.rahimjon.financewidget.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Same columns, in the same order, as the desktop app's export.
private val HEADER = listOf(
    "Ticker", "Shares", "Buy Price (original)", "Buy Price Currency", "Buy Price (USD)",
    "Current Price (USD)", "Market Value (USD)", "Cost Basis (USD)", "Gain/Loss (USD)",
    "Gain/Loss (%)", "Allocation (%)", "Buy Date", "Notes",
)

private val symbols = DecimalFormatSymbols(Locale.US)

// Plain decimals, never scientific notation, so spreadsheets read the numbers correctly.
private fun num(v: Double?, maxDecimals: Int = 8): String =
    v?.let { DecimalFormat("0." + "#".repeat(maxDecimals), symbols).format(it) } ?: ""

private fun field(text: String): String =
    if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${text.replace("\"", "\"\"")}\"" else text

fun buildCsv(rows: List<DerivedHolding>): String {
    val lines = mutableListOf(HEADER.joinToString(","))
    for (r in rows) {
        val h = r.holding
        lines += listOf(
            h.ticker,
            num(h.shares),
            num(h.buyPrice),
            h.buyPriceCurrency.name,
            num(r.buyPriceUsd),
            num(r.currentPriceUsd),
            num(r.marketValueUsd),
            num(r.costBasisUsd),
            num(r.gainLossUsd),
            num(r.gainLossPct, 2),
            num(r.allocationPct, 2),
            h.buyDate.orEmpty(),
            h.notes.orEmpty(),
        ).joinToString(",") { field(it) }
    }
    return lines.joinToString("\r\n") + "\r\n"
}
