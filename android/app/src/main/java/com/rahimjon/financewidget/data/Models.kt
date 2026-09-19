package com.rahimjon.financewidget.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class Currency { USD, KRW }

@Serializable
data class Holding(
    val id: String,
    val ticker: String,
    val shares: Double,
    val buyPrice: Double,
    val buyPriceCurrency: Currency = Currency.USD,
    val buyDate: String? = null,
    val notes: String? = null,
)

/** Editable fields of a [Holding]; the id is assigned by the store. */
data class HoldingInput(
    val ticker: String,
    val shares: Double,
    val buyPrice: Double,
    val buyPriceCurrency: Currency,
    val buyDate: String?,
    val notes: String?,
)

/**
 * A quote in the listing's own currency. [previousClose] is the close of the session
 * before the latest one (i.e. the last market day's close), null if unknown.
 */
@Serializable
data class Quote(
    val price: Double,
    val previousClose: Double?,
    val currency: String,
    val asOfMillis: Long,
)

/** Units of each currency per 1 USD, e.g. {"USD": 1.0, "KRW": 1388.28}. */
@Serializable
data class FxRates(
    val rates: Map<String, Double> = emptyMap(),
    val asOfMillis: Long = 0,
)

/** Which colours mean "up" and "down". Korean markets use red for up and blue for down. */
@Serializable
enum class ColorConvention { KOREAN, WESTERN }

@Serializable
data class Settings(
    val displayCurrency: Currency = Currency.USD,
    val refreshIntervalMinutes: Int = 30,
    val colorConvention: ColorConvention = ColorConvention.KOREAN,
)

@Serializable
data class AppState(
    val holdings: List<Holding> = emptyList(),
    val quotes: Map<String, Quote> = emptyMap(),
    val fx: FxRates = FxRates(),
    val settings: Settings = Settings(),
)

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}
