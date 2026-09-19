package com.rahimjon.financewidget

import android.app.Application
import com.rahimjon.financewidget.data.Refresher
import com.rahimjon.financewidget.data.RefreshResult
import com.rahimjon.financewidget.data.StateStore
import com.rahimjon.financewidget.data.UrlConnectionFetcher
import com.rahimjon.financewidget.data.YahooClient
import com.rahimjon.financewidget.widget.PortfolioWidgetProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class FinanceApp : Application() {
    val store: StateStore by lazy { StateStore(File(filesDir, "state.json")) }

    private val refresher = Refresher(YahooClient(UrlConnectionFetcher()))

    // Serialises refreshes (worker, widget button, pull-to-refresh). Each one reads the current
    // holdings when it starts, so a refresh queued behind another still sees newly added tickers.
    private val refreshLock = Mutex()

    suspend fun refreshQuotes(): RefreshResult = refreshLock.withLock {
        val result = refresher.refresh(store.snapshot())
        // Merge into the latest state rather than writing back the copy we started with.
        store.update { it.copy(quotes = result.quotes, fx = result.fx) }
        PortfolioWidgetProvider.notifyDataChanged(this)
        result
    }
}
