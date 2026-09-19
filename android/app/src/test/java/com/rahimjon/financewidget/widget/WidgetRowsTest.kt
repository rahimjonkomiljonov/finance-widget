package com.rahimjon.financewidget.widget

import com.rahimjon.financewidget.data.AppState
import com.rahimjon.financewidget.data.Currency
import com.rahimjon.financewidget.data.FxRates
import com.rahimjon.financewidget.data.Holding
import com.rahimjon.financewidget.data.Quote
import com.rahimjon.financewidget.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetRowsTest {
    private val fx = FxRates(mapOf("USD" to 1.0, "KRW" to 1400.0), asOfMillis = 1L)

    private fun state(
        vararg quotes: Pair<String, Quote>,
        tickers: List<String> = quotes.map { it.first },
        display: Currency = Currency.USD,
        rates: FxRates = fx,
    ) = AppState(
        holdings = tickers.map { Holding(id = it + tickers.indexOf(it), ticker = it, shares = 1.0, buyPrice = 1.0) },
        quotes = quotes.toMap(),
        fx = rates,
        settings = Settings(displayCurrency = display),
    )

    private fun quote(price: Double, prev: Double?, currency: String = "USD", at: Long = 0) =
        Quote(price, prev, currency, at)

    @Test
    fun `up move shows the change since the previous close`() {
        val row = WidgetRows.build(state("NVDA" to quote(222.27, 219.34))).single()

        assertEquals("NVDA", row.ticker)
        assertEquals("222.27", row.price)
        assertEquals("▲", row.arrow)
        assertEquals("2.93", row.change)
        assertEquals("1.34%", row.percent)
        assertEquals(Trend.UP, row.trend)
    }

    @Test
    fun `down move has a down arrow and an unsigned amount`() {
        val row = WidgetRows.build(state("AAPL" to quote(336.13, 337.0))).single()

        assertEquals("▼", row.arrow)
        assertEquals("0.87", row.change)
        assertEquals("0.26%", row.percent)
        assertEquals(Trend.DOWN, row.trend)
    }

    @Test
    fun `an unchanged price is flat, with no arrow direction or amount`() {
        val row = WidgetRows.build(state("KO" to quote(60.0, 60.0))).single()

        assertEquals(Trend.FLAT, row.trend)
        assertEquals("–", row.arrow)
        assertEquals("", row.change)
        assertEquals("0.00%", row.percent)
    }

    @Test
    fun `a move that rounds to zero percent is not shown as up or down`() {
        val row = WidgetRows.build(state("BRK" to quote(100.0001, 100.0))).single()
        assertEquals(Trend.FLAT, row.trend)
    }

    @Test
    fun `KRW display converts price and change`() {
        val row = WidgetRows.build(state("SPY" to quote(200.0, 190.0), display = Currency.KRW)).single()

        assertEquals("280,000", row.price)
        assertEquals("14,000", row.change)
        assertEquals("5.26%", row.percent)
    }

    @Test
    fun `KRW listing displayed in KRW round-trips to its native price and move`() {
        val row = WidgetRows.build(
            state("005930.KS" to quote(261_000.0, 252_500.0, currency = "KRW"), display = Currency.KRW),
        ).single()

        assertEquals("261,000", row.price)
        assertEquals("8,500", row.change)
        assertEquals("3.37%", row.percent)
        assertEquals(Trend.UP, row.trend)
    }

    @Test
    fun `KRW listing displayed in USD is converted`() {
        val row = WidgetRows.build(
            state("005930.KS" to quote(280_000.0, 266_000.0, currency = "KRW"), display = Currency.USD),
        ).single()

        assertEquals("200.00", row.price)
        assertEquals("10.00", row.change)
    }

    @Test
    fun `no quote yet shows a placeholder`() {
        val row = WidgetRows.build(state(tickers = listOf("NVDA"))).single()

        assertEquals("—", row.price)
        assertEquals(Trend.UNKNOWN, row.trend)
    }

    @Test
    fun `KRW display with no rate yet shows a placeholder instead of a wrong price`() {
        val row = WidgetRows.build(
            state("AAPL" to quote(200.0, 190.0), display = Currency.KRW, rates = FxRates()),
        ).single()

        assertEquals("—", row.price)
    }

    @Test
    fun `price known but no previous close shows price only`() {
        val row = WidgetRows.build(state("NEW" to quote(12.0, null))).single()

        assertEquals("12.00", row.price)
        assertEquals("", row.change)
        assertEquals(Trend.UNKNOWN, row.trend)
    }

    @Test
    fun `several lots of one ticker make one row`() {
        val s = state("AAPL" to quote(100.0, 99.0), tickers = listOf("AAPL", "AAPL", "MSFT"))
        assertEquals(listOf("AAPL", "MSFT"), WidgetRows.build(s).map { it.ticker })
    }

    @Test
    fun `rows keep holdings order`() {
        val s = state(
            "B" to quote(1.0, 1.0), "A" to quote(1.0, 1.0),
            tickers = listOf("B", "A"),
        )
        assertEquals(listOf("B", "A"), WidgetRows.build(s).map { it.ticker })
    }

    @Test
    fun `last updated is the newest quote of a held ticker`() {
        val s = state(
            "A" to quote(1.0, 1.0, at = 100), "B" to quote(1.0, 1.0, at = 300),
            "GONE" to quote(1.0, 1.0, at = 999), // cached, but no longer held
            tickers = listOf("A", "B"),
        )
        assertEquals(300L, WidgetRows.lastUpdatedMillis(s))
    }

    @Test
    fun `last updated is null before the first fetch`() {
        assertNull(WidgetRows.lastUpdatedMillis(state(tickers = listOf("A"))))
    }
}
