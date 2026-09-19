package com.rahimjon.financewidget.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private fun fixture(name: String): String =
    YahooClientTest::class.java.getResource("/$name")!!.readText()

class YahooClientTest {

    @Test
    fun `previous close is the daily bar before the latest, not Yahoo's chartPreviousClose`() {
        // Real AAPL payload: price 336.13; Thursday closed at 337.00. Yahoo's meta.chartPreviousClose
        // is the close *before the 10-day window*, which would give a wildly wrong day change.
        val quote = parseChart(fixture("chart_aapl.json"), nowMillis = 1L)

        assertEquals(336.13, quote.price, 1e-9)
        assertEquals(337.0, quote.previousClose!!, 1e-9)
        assertEquals("USD", quote.currency)
    }

    @Test
    fun `Korean listing is reported in its own currency`() {
        val quote = parseChart(fixture("chart_samsung.json"), nowMillis = 1L)

        assertEquals(261_000.0, quote.price, 1e-9)
        assertEquals(252_500.0, quote.previousClose!!, 1e-9)
        assertEquals("KRW", quote.currency)
    }

    @Test
    fun `exchange rate pair parses as units of currency per dollar`() {
        val quote = parseChart(fixture("chart_krw.json"), nowMillis = 1L)
        assertEquals(1385.95, quote.price, 1e-9)
    }

    @Test
    fun `unknown symbol surfaces Yahoo's message`() {
        try {
            parseChart(fixture("chart_notfound.json"), nowMillis = 1L)
            fail("expected an error")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!, e.message!!.contains("delisted"))
        }
    }

    @Test
    fun `a still-forming bar with a null close is skipped`() {
        val json = chartJson(price = 101.5, closes = "100.0, 101.0, null", chartPrev = 90.0)
        val quote = parseChart(json, nowMillis = 1L)

        // Non-null bars are [100, 101]; the previous close is the one before the latest.
        assertEquals(100.0, quote.previousClose!!, 1e-9)
    }

    @Test
    fun `a brand-new listing with one bar falls back to chartPreviousClose`() {
        val json = chartJson(price = 12.0, closes = "12.0", chartPrev = 10.0)
        assertEquals(10.0, parseChart(json, nowMillis = 1L).previousClose!!, 1e-9)
    }

    @Test
    fun `no bars and no chartPreviousClose leaves previous close unknown`() {
        val json = """{"chart":{"result":[{"meta":{"currency":"USD","regularMarketPrice":5.0},"indicators":{"quote":[{"close":[]}]}}],"error":null}}"""
        assertNull(parseChart(json, nowMillis = 1L).previousClose)
    }

    @Test
    fun `refresh merges fresh quotes and rates over the cache`() = runTest {
        val refresher = Refresher(YahooClient(routes(
            "AAPL" to fixture("chart_aapl.json"),
            "005930.KS" to fixture("chart_samsung.json"),
            "KRW%3DX" to fixture("chart_krw.json"),
        ), clock = { 5_000L }), clock = { 5_000L })

        val state = AppState(holdings = listOf(holding("AAPL"), holding("005930.KS")))
        val result = refresher.refresh(state)

        assertEquals(setOf("AAPL", "005930.KS"), result.quotes.keys)
        assertEquals(261_000.0, result.quotes.getValue("005930.KS").price, 1e-9)
        assertEquals(1385.95, result.fx.rates.getValue("KRW"), 1e-9)
        assertEquals(1.0, result.fx.rates.getValue("USD"), 1e-9)
        assertTrue(result.failedSymbols.isEmpty())
    }

    @Test
    fun `a failed ticker keeps its cached quote and is reported`() = runTest {
        val cached = Quote(price = 99.0, previousClose = 98.0, currency = "USD", asOfMillis = 1L)
        val refresher = Refresher(YahooClient(routes(
            "AAPL" to fixture("chart_aapl.json"),
            "KRW%3DX" to fixture("chart_krw.json"),
            // MSFT deliberately unroutable -> the fetch throws.
        )))

        val state = AppState(
            holdings = listOf(holding("AAPL"), holding("MSFT")),
            quotes = mapOf("MSFT" to cached),
        )
        val result = refresher.refresh(state)

        assertEquals(cached, result.quotes["MSFT"])
        assertEquals(336.13, result.quotes.getValue("AAPL").price, 1e-9)
        assertEquals(listOf("MSFT"), result.failedSymbols)
    }

    @Test
    fun `a failed rate lookup keeps the previous rates`() = runTest {
        val refresher = Refresher(YahooClient(routes("AAPL" to fixture("chart_aapl.json"))))
        val old = FxRates(mapOf("USD" to 1.0, "KRW" to 1300.0), asOfMillis = 7L)

        val result = refresher.refresh(AppState(holdings = listOf(holding("AAPL")), fx = old))

        assertEquals(old, result.fx)
        assertTrue(result.failedSymbols.contains("KRW=X"))
    }

    @Test
    fun `non-USD listing currencies get their own rate`() = runTest {
        val requested = mutableListOf<String>()
        val fetcher = HttpFetcher { url ->
            requested += url
            when {
                "005930.KS" in url -> fixture("chart_samsung.json")
                "KRW%3DX" in url -> fixture("chart_krw.json")
                else -> error("unexpected $url")
            }
        }
        Refresher(YahooClient(fetcher)).refresh(AppState(holdings = listOf(holding("005930.KS"))))

        // KRW is needed both for the display toggle and the listing; it must be asked for once.
        assertEquals(1, requested.count { "KRW%3DX" in it })
    }

    private fun holding(ticker: String) = Holding(id = ticker, ticker = ticker, shares = 1.0, buyPrice = 1.0)

    private fun routes(vararg pairs: Pair<String, String>) = HttpFetcher { url ->
        pairs.firstOrNull { (key, _) -> url.contains("/chart/$key?") }?.second ?: error("no route for $url")
    }

    private fun chartJson(price: Double, closes: String, chartPrev: Double) =
        """{"chart":{"result":[{"meta":{"currency":"USD","regularMarketPrice":$price,"chartPreviousClose":$chartPrev},""" +
            """"indicators":{"quote":[{"close":[$closes]}]}}],"error":null}}"""
}
