package com.rahimjon.financewidget.widget

import com.rahimjon.financewidget.data.AppState
import com.rahimjon.financewidget.data.formatAmount
import com.rahimjon.financewidget.data.formatPercentUnsigned
import com.rahimjon.financewidget.data.fromUsd
import com.rahimjon.financewidget.data.toUsd
import kotlin.math.abs

enum class Trend { UP, DOWN, FLAT, UNKNOWN }

/** One line of the home-screen widget: price and the move since the previous market close. */
data class WidgetRow(
    val ticker: String,
    val price: String,
    val arrow: String,
    val change: String,
    val percent: String,
    val trend: Trend,
)

object WidgetRows {
    fun build(state: AppState): List<WidgetRow> {
        val currency = state.settings.displayCurrency
        val rates = state.fx.rates

        // One row per ticker: several buy lots of the same stock share a price and a day move.
        return state.holdings.map { it.ticker }.distinct().map { ticker ->
            val quote = state.quotes[ticker]
            val priceUsd = quote?.let { toUsd(it.price, it.currency, rates) }
            val price = priceUsd?.let { fromUsd(it, currency, rates) }
                ?: return@map WidgetRow(ticker, "—", "", "", "", Trend.UNKNOWN)

            val prevUsd = quote.previousClose?.let { toUsd(it, quote.currency, rates) }?.takeIf { it > 0 }
            // (change in USD, change in percent) — both derived from the same previous close.
            val move = prevUsd?.let { prev -> (priceUsd - prev) to ((priceUsd - prev) / prev * 100) }
            val change = move?.first?.let { fromUsd(it, currency, rates) }
            val pct = move?.second

            if (change == null || pct == null) {
                return@map WidgetRow(ticker, formatAmount(price, currency), "", "", "", Trend.UNKNOWN)
            }

            // Judge "flat" by what the user would see, so a +0.00% never gets an up arrow.
            val shownPct = formatPercentUnsigned(pct)
            val trend = when {
                shownPct == "0.00%" -> Trend.FLAT
                pct > 0 -> Trend.UP
                else -> Trend.DOWN
            }
            WidgetRow(
                ticker = ticker,
                price = formatAmount(price, currency),
                arrow = when (trend) { Trend.UP -> "▲"; Trend.DOWN -> "▼"; else -> "–" },
                change = if (trend == Trend.FLAT) "" else formatAmount(abs(change), currency),
                percent = shownPct,
                trend = trend,
            )
        }
    }

    /** Newest quote time among the held tickers, or null if nothing has been fetched yet. */
    fun lastUpdatedMillis(state: AppState): Long? =
        state.holdings.map { it.ticker }.distinct().mapNotNull { state.quotes[it]?.asOfMillis }.maxOrNull()
}
