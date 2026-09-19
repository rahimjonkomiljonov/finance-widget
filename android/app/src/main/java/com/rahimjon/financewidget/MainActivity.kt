package com.rahimjon.financewidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rahimjon.financewidget.ui.FinanceTheme
import com.rahimjon.financewidget.ui.HomeScreen
import com.rahimjon.financewidget.widget.PortfolioWidgetProvider
import com.rahimjon.financewidget.work.RefreshScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (PortfolioWidgetProvider.hasWidgets(this)) {
            // WorkManager keeps periodic work across reboots, but re-asserting it is cheap and
            // covers the case where the widget was added before an update reset the schedule.
            val minutes = (application as FinanceApp).store.snapshot().settings.refreshIntervalMinutes
            RefreshScheduler.schedule(this, minutes)
            // Force-stopping an app cancels its widgets' tap targets; repainting on launch restores them.
            PortfolioWidgetProvider.notifyDataChanged(this)
        }

        setContent { FinanceTheme { HomeScreen() } }
    }
}
