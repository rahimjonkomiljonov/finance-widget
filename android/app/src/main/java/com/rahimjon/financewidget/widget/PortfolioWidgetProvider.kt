package com.rahimjon.financewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.rahimjon.financewidget.FinanceApp
import com.rahimjon.financewidget.MainActivity
import com.rahimjon.financewidget.R
import com.rahimjon.financewidget.work.RefreshScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PortfolioWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { manager.updateAppWidget(it, buildViews(context, it)) }
        manager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
    }

    override fun onEnabled(context: Context) {
        val minutes = (context.applicationContext as FinanceApp).store.snapshot().settings.refreshIntervalMinutes
        RefreshScheduler.schedule(context, minutes)
        RefreshScheduler.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        RefreshScheduler.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            // Immediate feedback that the tap registered; the worker repaints when it finishes.
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PortfolioWidgetProvider::class.java))
            ids.forEach { manager.partiallyUpdateAppWidget(it, RemoteViews(context.packageName, R.layout.widget_portfolio).apply {
                setTextViewText(R.id.widget_time, context.getString(R.string.widget_updating))
            }) }
            RefreshScheduler.refreshNow(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.rahimjon.financewidget.action.REFRESH"

        fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, PortfolioWidgetProvider::class.java)).isNotEmpty()

        /** Asks the launcher to show its "add widget to home screen" prompt. False if unsupported. */
        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            return manager.requestPinAppWidget(ComponentName(context, PortfolioWidgetProvider::class.java), null, null)
        }

        /** Repaints every widget instance from the current state (call after any data change). */
        fun notifyDataChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PortfolioWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { manager.updateAppWidget(it, buildViews(context, it)) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
            val state = (context.applicationContext as FinanceApp).store.snapshot()
            val views = RemoteViews(context.packageName, R.layout.widget_portfolio)

            views.setTextViewText(R.id.widget_currency, state.settings.displayCurrency.name)
            val updated = WidgetRows.lastUpdatedMillis(state)
            views.setTextViewText(
                R.id.widget_time,
                updated?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: "--:--",
            )

            // A unique data URI per widget keeps each instance's adapter separate.
            val adapterIntent = Intent(context, WidgetListService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, adapterIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)
            // The empty view also shows while the list is still loading, so only claim
            // "No stocks yet" when that is actually true.
            views.setTextViewText(
                R.id.widget_empty,
                context.getString(if (state.holdings.isEmpty()) R.string.widget_empty else R.string.widget_loading),
            )

            val openApp = openAppIntent(context)
            // Rows inherit this template and fill in nothing extra: any tap opens the app.
            val templateFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            views.setPendingIntentTemplate(R.id.widget_list, PendingIntent.getActivity(context, 0, openApp, templateFlags))
            val openAppPending = PendingIntent.getActivity(
                context, 1, openApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_header, openAppPending)
            views.setOnClickPendingIntent(R.id.widget_empty, openAppPending)

            val refresh = Intent(context, PortfolioWidgetProvider::class.java).setAction(ACTION_REFRESH)
            views.setOnClickPendingIntent(
                R.id.widget_refresh,
                PendingIntent.getBroadcast(context, 2, refresh, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
            return views
        }

        private fun openAppIntent(context: Context) =
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
