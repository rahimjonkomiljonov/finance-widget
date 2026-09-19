package com.rahimjon.financewidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatAndCsvTest {

    @Test
    fun `USD has two decimals and a dollar sign`() {
        assertEquals("$1,234.50", formatMoney(1234.5, Currency.USD))
        assertEquals("$0.00", formatMoney(0.0, Currency.USD))
    }

    @Test
    fun `KRW has no decimals and a won sign`() {
        assertEquals("₩1,234,567", formatMoney(1_234_567.4, Currency.KRW))
    }

    @Test
    fun `negative amounts put the minus before the symbol`() {
        assertEquals("-$12.00", formatMoney(-12.0, Currency.USD))
        assertEquals("-₩43,500", formatMoney(-43_500.0, Currency.KRW))
    }

    @Test
    fun `an amount that rounds to zero never shows a minus sign`() {
        assertEquals("$0.00", formatMoney(-0.001, Currency.USD))
    }

    @Test
    fun `percent is signed, and zero has no sign`() {
        assertEquals("+1.23%", formatPercent(1.234))
        assertEquals("-14.29%", formatPercent(-14.2857))
        assertEquals("0.00%", formatPercent(0.0))
        assertEquals("0.00%", formatPercent(-0.004))
        assertEquals("—", formatPercent(null))
    }

    @Test
    fun `share counts drop trailing zeros`() {
        assertEquals("10", formatShares(10.0))
        assertEquals("0.5", formatShares(0.5))
        assertEquals("1,234.5678", formatShares(1234.5678))
    }

    @Test
    fun `plain numbers round-trip for editing`() {
        assertEquals("150", formatPlain(150.0))
        assertEquals("0.25", formatPlain(0.25))
        assertEquals(304_500.0, formatPlain(304_500.0).toDouble(), 0.0)
    }

    @Test
    fun `formatting ignores the phone's locale`() {
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY) // decimal comma
            assertEquals("$1,234.50", formatMoney(1234.5, Currency.USD))
        } finally {
            java.util.Locale.setDefault(previous)
        }
    }

    // ---- CSV -------------------------------------------------------------------------------

    private fun derived(h: Holding) = deriveHoldings(
        listOf(h),
        mapOf(h.ticker to Quote(200.0, 190.0, "USD", 0)),
        mapOf("USD" to 1.0, "KRW" to 1400.0),
    ).single()

    @Test
    fun `csv uses the desktop columns and CRLF line ends`() {
        val csv = buildCsv(listOf(derived(Holding("1", "AAPL", 2.0, 150.0))))
        val lines = csv.split("\r\n")

        assertEquals(
            "Ticker,Shares,Buy Price (original),Buy Price Currency,Buy Price (USD),Current Price (USD)," +
                "Market Value (USD),Cost Basis (USD),Gain/Loss (USD),Gain/Loss (%),Allocation (%),Buy Date,Notes",
            lines[0],
        )
        assertEquals("AAPL,2,150,USD,150,200,400,300,100,33.33,100,,", lines[1])
        assertEquals("", lines.last()) // trailing newline
    }

    @Test
    fun `csv keeps the original KRW buy price and its converted USD value`() {
        val h = Holding("1", "005930.KS", 1.0, 304_500.0, Currency.KRW, buyDate = "2026-06-08")
        val row = buildCsv(listOf(derived(h))).split("\r\n")[1].split(",")

        assertEquals("304500", row[2])
        assertEquals("KRW", row[3])
        assertEquals("2026-06-08", row[11])
    }

    @Test
    fun `csv quotes fields containing commas, quotes and newlines`() {
        val h = Holding("1", "AAPL", 1.0, 1.0, notes = "said \"buy\", then\nwaited")
        val csv = buildCsv(listOf(derived(h)))

        assertTrue(csv, csv.contains("\"said \"\"buy\"\", then\nwaited\""))
    }

    @Test
    fun `csv never uses scientific notation`() {
        val h = Holding("1", "PENNY", 0.00000001, 0.0000005)
        val csv = buildCsv(listOf(derived(h)))

        assertTrue(csv, !csv.contains("E-"))
    }

    @Test
    fun `csv leaves unknown values blank`() {
        val h = Holding("1", "NEW", 1.0, 5.0)
        val csv = buildCsv(deriveHoldings(listOf(h), emptyMap(), mapOf("USD" to 1.0)))

        assertEquals("NEW,1,5,USD,5,,,5,,,,,", csv.split("\r\n")[1])
    }
}
