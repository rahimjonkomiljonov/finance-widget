package com.rahimjon.financewidget.data

data class DerivedHolding(
    val holding: Holding,
    val buyPriceUsd: Double?,
    val currentPriceUsd: Double?,
    val marketValueUsd: Double?,
    val costBasisUsd: Double?,
    val gainLossUsd: Double?,
    val gainLossPct: Double?,
    val allocationPct: Double?,
    /** Per-share move since the previous close, in USD. */
    val dayChangeUsd: Double?,
    val dayChangePct: Double?,
)

data class PortfolioTotals(
    val marketValueUsd: Double,
    val costBasisUsd: Double,
    val gainLossUsd: Double,
    val gainLossPct: Double?,
    /** Whole-portfolio move since the previous close (shares x per-share change). */
    val dayChangeUsd: Double? = null,
    val dayChangePct: Double? = null,
)

/**
 * Converts an amount to USD. Null when no rate is known for the currency, so an
 * unconvertible value surfaces as "—" rather than a silently wrong number.
 */
fun toUsd(amount: Double, currency: String, rates: Map<String, Double>): Double? {
    if (currency == "USD") return amount
    val rate = rates[currency]?.takeIf { it > 0 } ?: return null
    return amount / rate
}

/** Converts a USD amount into the currency the user chose to see. */
fun fromUsd(usd: Double, currency: Currency, rates: Map<String, Double>): Double? =
    when (currency) {
        Currency.USD -> usd
        Currency.KRW -> rates["KRW"]?.takeIf { it > 0 }?.let { usd * it }
    }

fun deriveHoldings(
    holdings: List<Holding>,
    quotes: Map<String, Quote>,
    rates: Map<String, Double>,
): List<DerivedHolding> {
    val withValues = holdings.map { h ->
        val quote = quotes[h.ticker]
        // Quotes are in the listing's own currency (005930.KS is KRW), so convert both
        // the price and the previous close with the same rate to keep FX noise out of the day move.
        val currentUsd = quote?.let { toUsd(it.price, it.currency, rates) }
        val prevUsd = quote?.previousClose?.let { toUsd(it, quote.currency, rates) }
        val buyUsd = toUsd(h.buyPrice, h.buyPriceCurrency.name, rates)

        val marketValue = currentUsd?.let { h.shares * it }
        val costBasis = buyUsd?.let { h.shares * it }
        val gainLoss = if (marketValue != null && costBasis != null) marketValue - costBasis else null
        val dayChange = if (currentUsd != null && prevUsd != null) currentUsd - prevUsd else null

        DerivedHolding(
            holding = h,
            buyPriceUsd = buyUsd,
            currentPriceUsd = currentUsd,
            marketValueUsd = marketValue,
            costBasisUsd = costBasis,
            gainLossUsd = gainLoss,
            gainLossPct = if (gainLoss != null && costBasis != null && costBasis > 0) gainLoss / costBasis * 100 else null,
            allocationPct = null,
            dayChangeUsd = dayChange,
            dayChangePct = if (dayChange != null && prevUsd != null && prevUsd > 0) dayChange / prevUsd * 100 else null,
        )
    }

    val total = withValues.sumOf { it.marketValueUsd ?: 0.0 }
    return withValues.map {
        it.copy(allocationPct = if (total > 0 && it.marketValueUsd != null) it.marketValueUsd / total * 100 else null)
    }
}

fun computeTotals(derived: List<DerivedHolding>): PortfolioTotals {
    val value = derived.sumOf { it.marketValueUsd ?: 0.0 }

    // Gain is measured only over holdings that have both a price and a cost basis. Otherwise a
    // holding whose quote hasn't loaded yet counts its full cost as a loss (a fake -100%).
    val priced = derived.filter { it.marketValueUsd != null && it.costBasisUsd != null }
    val cost = priced.sumOf { it.costBasisUsd ?: 0.0 }
    val gain = priced.sumOf { (it.marketValueUsd ?: 0.0) - (it.costBasisUsd ?: 0.0) }

    // Same rule for the day move: only holdings that have both a price and a previous close.
    val moved = derived.filter { it.dayChangeUsd != null && it.currentPriceUsd != null }
    val dayChange = if (moved.isEmpty()) null else moved.sumOf { it.holding.shares * (it.dayChangeUsd ?: 0.0) }
    val dayBase = moved.sumOf { it.holding.shares * ((it.currentPriceUsd ?: 0.0) - (it.dayChangeUsd ?: 0.0)) }

    return PortfolioTotals(
        marketValueUsd = value,
        costBasisUsd = cost,
        gainLossUsd = gain,
        gainLossPct = if (cost > 0) gain / cost * 100 else null,
        dayChangeUsd = dayChange,
        dayChangePct = if (dayChange != null && dayBase > 0) dayChange / dayBase * 100 else null,
    )
}
