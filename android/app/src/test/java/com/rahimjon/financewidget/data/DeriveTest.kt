package com.rahimjon.financewidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeriveTest {
    private val rates = mapOf("USD" to 1.0, "KRW" to 1400.0)

    private fun holding(
        ticker: String,
        shares: Double = 1.0,
        buy: Double,
        buyCurrency: Currency = Currency.USD,
    ) = Holding(id = ticker, ticker = ticker, shares = shares, buyPrice = buy, buyPriceCurrency = buyCurrency)

    private fun quote(price: Double, prev: Double?, currency: String) =
        Quote(price = price, previousClose = prev, currency = currency, asOfMillis = 0)

    @Test
    fun `a KRW listing bought in KRW is a small loss, not a giant gain`() {
        // The desktop app once treated the 261,000 won quote as $261,000 (+37765%).
        val derived = deriveHoldings(
            holdings = listOf(holding("005930.KS", buy = 304_500.0, buyCurrency = Currency.KRW)),
            quotes = mapOf("005930.KS" to quote(261_000.0, 252_500.0, "KRW")),
            rates = rates,
        ).single()

        assertEquals(261_000.0 / 1400, derived.currentPriceUsd!!, 1e-9)
        assertEquals(-14.286, derived.gainLossPct!!, 0.001)
    }

    @Test
    fun `a USD buy of a KRW listing converts each side with the same rate`() {
        val derived = deriveHoldings(
            listOf(holding("005930.KS", buy = 200.0, buyCurrency = Currency.USD)),
            mapOf("005930.KS" to quote(280_000.0, 270_000.0, "KRW")),
            rates,
        ).single()

        // 280,000 KRW = $200 at 1400, so no gain or loss.
        assertEquals(0.0, derived.gainLossUsd!!, 1e-9)
    }

    @Test
    fun `day change compares the price with the previous close in the same currency`() {
        val derived = deriveHoldings(
            listOf(holding("AAPL", buy = 100.0)),
            mapOf("AAPL" to quote(336.13, 337.0, "USD")),
            rates,
        ).single()

        assertEquals(-0.87, derived.dayChangeUsd!!, 1e-9)
        assertEquals(-0.258, derived.dayChangePct!!, 0.001)
    }

    @Test
    fun `day change on a KRW listing does not pick up FX noise`() {
        val derived = deriveHoldings(
            listOf(holding("005930.KS", buy = 1.0)),
            mapOf("005930.KS" to quote(261_000.0, 252_500.0, "KRW")),
            rates,
        ).single()

        assertEquals(3.366, derived.dayChangePct!!, 0.001)
    }

    @Test
    fun `an unknown exchange rate gives null, never a wrong number`() {
        val derived = deriveHoldings(
            listOf(holding("005930.KS", buy = 304_500.0, buyCurrency = Currency.KRW)),
            mapOf("005930.KS" to quote(261_000.0, null, "KRW")),
            rates = mapOf("USD" to 1.0),
        ).single()

        assertNull(derived.currentPriceUsd)
        assertNull(derived.buyPriceUsd)
        assertNull(derived.gainLossPct)
    }

    @Test
    fun `holding without a quote yet has no value`() {
        val derived = deriveHoldings(listOf(holding("NVDA", buy = 100.0)), emptyMap(), rates).single()

        assertNull(derived.marketValueUsd)
        assertNotNull(derived.costBasisUsd)
        assertNull(derived.gainLossUsd)
    }

    @Test
    fun `totals ignore cost of holdings that have no price yet`() {
        // Regression: counting NVDA's cost with no price produced a fake -100% while loading.
        val derived = deriveHoldings(
            listOf(holding("AAPL", buy = 100.0), holding("NVDA", buy = 500.0)),
            mapOf("AAPL" to quote(150.0, 148.0, "USD")),
            rates,
        )
        val totals = computeTotals(derived)

        assertEquals(150.0, totals.marketValueUsd, 1e-9)
        assertEquals(50.0, totals.gainLossUsd, 1e-9)
        assertEquals(50.0, totals.gainLossPct!!, 1e-9)
    }

    @Test
    fun `portfolio day change weights each holding by its share count`() {
        val derived = deriveHoldings(
            listOf(holding("A", shares = 10.0, buy = 1.0), holding("B", shares = 1.0, buy = 1.0)),
            mapOf(
                "A" to quote(110.0, 100.0, "USD"), // +10 x 10 shares = +100
                "B" to quote(90.0, 100.0, "USD"), //  -10 x 1 share  = -10
            ),
            rates,
        )
        val totals = computeTotals(derived)

        assertEquals(90.0, totals.dayChangeUsd!!, 1e-9)
        // Base = previous-close value: 10*100 + 1*100 = 1100
        assertEquals(90.0 / 1100 * 100, totals.dayChangePct!!, 1e-9)
    }

    @Test
    fun `allocation percentages add up to 100`() {
        val derived = deriveHoldings(
            listOf(holding("A", buy = 1.0), holding("B", buy = 1.0), holding("C", buy = 1.0)),
            mapOf("A" to quote(50.0, 1.0, "USD"), "B" to quote(30.0, 1.0, "USD"), "C" to quote(20.0, 1.0, "USD")),
            rates,
        )
        assertEquals(100.0, derived.sumOf { it.allocationPct!! }, 1e-9)
    }

    @Test
    fun `converting to the display currency`() {
        assertEquals(140_000.0, fromUsd(100.0, Currency.KRW, rates)!!, 1e-9)
        assertEquals(100.0, fromUsd(100.0, Currency.USD, rates)!!, 1e-9)
        assertNull(fromUsd(100.0, Currency.KRW, mapOf("USD" to 1.0)))
    }
}
