package com.rahimjon.financewidget.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rahimjon.financewidget.FinanceApp
import com.rahimjon.financewidget.data.AppState
import com.rahimjon.financewidget.data.ColorConvention
import com.rahimjon.financewidget.data.Currency
import com.rahimjon.financewidget.data.DerivedHolding
import com.rahimjon.financewidget.data.Holding
import com.rahimjon.financewidget.data.HoldingInput
import com.rahimjon.financewidget.data.PortfolioTotals
import com.rahimjon.financewidget.data.Settings
import com.rahimjon.financewidget.data.buildCsv
import com.rahimjon.financewidget.data.computeTotals
import com.rahimjon.financewidget.data.deriveHoldings
import com.rahimjon.financewidget.widget.PortfolioWidgetProvider
import com.rahimjon.financewidget.widget.WidgetRows
import com.rahimjon.financewidget.work.RefreshScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class UiState(
    val derived: List<DerivedHolding>,
    val totals: PortfolioTotals,
    val settings: Settings,
    val rates: Map<String, Double>,
    val lastUpdatedMillis: Long?,
    val refreshing: Boolean,
)

private fun buildUi(state: AppState, refreshing: Boolean): UiState {
    val derived = deriveHoldings(state.holdings, state.quotes, state.fx.rates)
    return UiState(
        derived = derived,
        totals = computeTotals(derived),
        settings = state.settings,
        rates = state.fx.rates,
        lastUpdatedMillis = WidgetRows.lastUpdatedMillis(state),
        refreshing = refreshing,
    )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FinanceApp
    private val refreshing = MutableStateFlow(false)
    private val messageChannel = Channel<String>(Channel.BUFFERED)

    val messages = messageChannel.receiveAsFlow()

    val ui: StateFlow<UiState> = combine(app.store.state, refreshing, ::buildUi)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildUi(app.store.snapshot(), false))

    /** Applies a change and repaints the home-screen widget, which reads the same state. */
    private fun mutate(transform: (AppState) -> AppState) {
        app.store.update(transform)
        PortfolioWidgetProvider.notifyDataChanged(app)
    }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            val result = runCatching { app.refreshQuotes() }
            refreshing.value = false
            result.onFailure { messageChannel.trySend("Couldn't refresh prices") }
            result.onSuccess { r ->
                if (r.failedSymbols.isNotEmpty()) {
                    val what = r.failedSymbols.map { if (it.endsWith("=X")) "exchange rate" else it }.distinct()
                    messageChannel.trySend("Couldn't update: ${what.joinToString()}")
                }
            }
        }
    }

    fun refreshIfStale() {
        val last = ui.value.lastUpdatedMillis
        if (last == null || System.currentTimeMillis() - last > STALE_AFTER_MILLIS) refresh()
    }

    fun addHolding(input: HoldingInput) {
        mutate { it.copy(holdings = it.holdings + input.toHolding(UUID.randomUUID().toString())) }
        refresh() // the new ticker has no quote yet
    }

    fun updateHolding(id: String, input: HoldingInput) {
        mutate { s -> s.copy(holdings = s.holdings.map { if (it.id == id) input.toHolding(id) else it }) }
        refresh()
    }

    fun deleteHolding(id: String) = mutate { s -> s.copy(holdings = s.holdings.filterNot { it.id == id }) }

    fun setCurrency(currency: Currency) = mutate { it.copy(settings = it.settings.copy(displayCurrency = currency)) }

    fun setColorConvention(convention: ColorConvention) =
        mutate { it.copy(settings = it.settings.copy(colorConvention = convention)) }

    fun setRefreshInterval(minutes: Int) {
        mutate { it.copy(settings = it.settings.copy(refreshIntervalMinutes = minutes)) }
        if (PortfolioWidgetProvider.hasWidgets(app)) RefreshScheduler.schedule(app, minutes)
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val out = app.contentResolver.openOutputStream(uri, "wt") ?: error("Can't open file")
                    out.use { it.write(buildCsv(ui.value.derived).toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            messageChannel.trySend(if (ok) "Exported to CSV" else "Couldn't save the file")
        }
    }

    fun addWidgetToHomeScreen() {
        if (!PortfolioWidgetProvider.requestPin(app)) {
            messageChannel.trySend("Long-press your home screen and choose Widgets to add it")
        }
    }

    private companion object {
        const val STALE_AFTER_MILLIS = 60_000L
    }
}

private fun HoldingInput.toHolding(id: String) = Holding(
    id = id,
    ticker = ticker.trim().uppercase(),
    shares = shares,
    buyPrice = buyPrice,
    buyPriceCurrency = buyPriceCurrency,
    buyDate = buyDate,
    notes = notes,
)
