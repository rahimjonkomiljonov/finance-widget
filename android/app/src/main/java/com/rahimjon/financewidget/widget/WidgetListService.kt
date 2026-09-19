package com.rahimjon.financewidget.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.rahimjon.financewidget.FinanceApp
import com.rahimjon.financewidget.R
import com.rahimjon.financewidget.data.ColorConvention

class WidgetListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = RowsFactory(applicationContext)
}

private class RowsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<WidgetRow> = emptyList()
    private var convention = ColorConvention.KOREAN

    // Called by the system on a binder thread whenever notifyAppWidgetViewDataChanged fires.
    override fun onDataSetChanged() {
        val state = (context.applicationContext as FinanceApp).store.snapshot()
        rows = WidgetRows.build(state)
        convention = state.settings.colorConvention
    }

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_row)
        val trendColor = ContextCompat.getColor(context, trendColorRes(row.trend, convention))
        return RemoteViews(context.packageName, R.layout.widget_row).apply {
            setTextViewText(R.id.row_name, row.ticker)
            setTextViewText(R.id.row_price, row.price)
            setTextViewText(R.id.row_arrow, row.arrow)
            setTextViewText(R.id.row_change, row.change)
            setTextViewText(R.id.row_percent, row.percent)
            listOf(R.id.row_price, R.id.row_arrow, R.id.row_change, R.id.row_percent).forEach {
                setTextColor(it, trendColor)
            }
            // The empty fill-in intent merges into the template, so a tap anywhere on the row opens the app.
            setOnClickFillInIntent(R.id.row_root, Intent())
        }
    }

    override fun onCreate() = onDataSetChanged()
    override fun onDestroy() {}
    override fun getCount() = rows.size
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = position.toLong()
    override fun hasStableIds() = true
}

fun trendColorRes(trend: Trend, convention: ColorConvention): Int = when (convention) {
    ColorConvention.KOREAN -> when (trend) {
        Trend.UP -> R.color.trend_up_kr
        Trend.DOWN -> R.color.trend_down_kr
        else -> R.color.widget_text_secondary
    }
    ColorConvention.WESTERN -> when (trend) {
        Trend.UP -> R.color.trend_up_we
        Trend.DOWN -> R.color.trend_down_we
        else -> R.color.widget_text_secondary
    }
}
