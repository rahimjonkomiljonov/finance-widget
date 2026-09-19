package com.rahimjon.financewidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun interface HttpFetcher {
    suspend fun get(url: String): String
}

class UrlConnectionFetcher : HttpFetcher {
    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            // Yahoo rejects requests without a browser-like User-Agent.
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code for $url")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

@Serializable
private data class ChartResponse(val chart: ChartBody)

@Serializable
private data class ChartBody(val result: List<ChartResult>? = null, val error: ChartError? = null)

@Serializable
private data class ChartError(val code: String? = null, val description: String? = null)

@Serializable
private data class ChartResult(val meta: ChartMeta, val indicators: Indicators? = null)

@Serializable
private data class ChartMeta(
    val currency: String? = null,
    val regularMarketPrice: Double? = null,
    val chartPreviousClose: Double? = null,
)

@Serializable
private data class Indicators(val quote: List<QuoteSeries> = emptyList())

@Serializable
private data class QuoteSeries(val close: List<Double?> = emptyList())

/**
 * Parses a Yahoo `v8/finance/chart` response (daily bars).
 *
 * `meta.chartPreviousClose` is NOT the previous session's close when a multi-day range is
 * requested (it is the close before the first bar). The correct previous close is the daily
 * bar before the latest one: the latest bar is the current or just-finished session.
 */
fun parseChart(json: String, nowMillis: Long): Quote {
    val body = AppJson.decodeFromString<ChartResponse>(json).chart
    val result = body.result?.firstOrNull()
        ?: error(body.error?.description ?: "No chart data")

    val closes = result.indicators?.quote?.firstOrNull()?.close.orEmpty().filterNotNull()
    val price = result.meta.regularMarketPrice ?: closes.lastOrNull() ?: error("No price in chart data")
    val previousClose = if (closes.size >= 2) closes[closes.size - 2] else result.meta.chartPreviousClose

    return Quote(
        price = price,
        previousClose = previousClose,
        currency = result.meta.currency ?: "USD",
        asOfMillis = nowMillis,
    )
}

class YahooClient(
    private val http: HttpFetcher,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun fetchQuote(symbol: String): Quote =
        parseChart(http.get(chartUrl(symbol)), clock())

    /** Units of [currency] per 1 USD, via Yahoo's "<CUR>=X" pair. */
    suspend fun fetchRate(currency: String): Double =
        parseChart(http.get(chartUrl("$currency=X")), clock()).price

    private fun chartUrl(symbol: String): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        // 10d, not 5d: multi-day market holidays (e.g. Chuseok) can leave <2 bars in a short window.
        return "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?range=10d&interval=1d"
    }
}

/**
 * Only quotes and rates: callers merge these into the *latest* state, so edits the user made
 * while the network calls were in flight are never overwritten by a stale copy.
 */
data class RefreshResult(val quotes: Map<String, Quote>, val fx: FxRates, val failedSymbols: List<String>)

/** Fetches quotes and FX rates for the holdings and merges them over the cached ones. */
class Refresher(private val client: YahooClient, private val clock: () -> Long = System::currentTimeMillis) {

    suspend fun refresh(current: AppState): RefreshResult = coroutineScope {
        val tickers = current.holdings.map { it.ticker }.distinct()
        val gate = Semaphore(4)

        val quoteResults = tickers.map { t ->
            async { t to gate.withPermit { runCatching { client.fetchQuote(t) } } }
        }.awaitAll()

        val quotes = current.quotes.toMutableMap()
        val failed = mutableListOf<String>()
        for ((ticker, result) in quoteResults) {
            result.onSuccess { quotes[ticker] = it }.onFailure { failed += ticker }
        }

        // KRW is always needed for the display toggle, plus every non-USD listing currency.
        val currencies = (setOf("KRW") + tickers.mapNotNull { quotes[it]?.currency }).filter { it != "USD" }.toSet()
        val rateResults = currencies.map { c ->
            async { c to gate.withPermit { runCatching { client.fetchRate(c) } } }
        }.awaitAll()

        // Merge so a partial failure never drops a rate we already had.
        val rates = current.fx.rates.toMutableMap().apply { put("USD", 1.0) }
        var gotRate = false
        for ((currency, result) in rateResults) {
            result.onSuccess { rates[currency] = it; gotRate = true }.onFailure { failed += "$currency=X" }
        }

        val fx = if (gotRate) FxRates(rates, clock()) else current.fx
        RefreshResult(quotes, fx, failed)
    }
}
