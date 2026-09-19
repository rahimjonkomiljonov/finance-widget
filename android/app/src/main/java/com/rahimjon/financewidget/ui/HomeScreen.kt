package com.rahimjon.financewidget.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahimjon.financewidget.data.ColorConvention
import com.rahimjon.financewidget.data.Currency
import com.rahimjon.financewidget.data.DerivedHolding
import com.rahimjon.financewidget.data.formatMoney
import com.rahimjon.financewidget.data.formatPercent
import com.rahimjon.financewidget.data.formatShares
import com.rahimjon.financewidget.data.fromUsd
import com.rahimjon.financewidget.widget.Trend
import com.rahimjon.financewidget.widget.trendColorRes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface DialogState {
    data object Add : DialogState
    data class Edit(val holding: DerivedHolding) : DialogState
    data class Delete(val holding: DerivedHolding) : DialogState
    data object Settings : DialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<DialogState?>(null) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    // Fresh prices whenever the app comes to the front (e.g. after tapping the widget).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshIfStale() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(vm::exportCsv)
    }

    val currency = ui.settings.displayCurrency
    val convention = ui.settings.colorConvention
    fun money(usd: Double?): String =
        usd?.let { fromUsd(it, currency, ui.rates) }?.let { formatMoney(it, currency) } ?: "—"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Widget", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !ui.refreshing) { Icon(Icons.Default.Refresh, "Refresh prices") }
                    IconButton(onClick = { dialog = DialogState.Settings }) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = DialogState.Add },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add stock") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = vm::refresh, modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                            Currency.entries.forEachIndexed { index, c ->
                                SegmentedButton(
                                    selected = currency == c,
                                    onClick = { vm.setCurrency(c) },
                                    shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size),
                                ) { Text(c.name) }
                            }
                        }
                    }
                }

                if (ui.derived.isEmpty()) {
                    item { EmptyState() }
                } else {
                    item { TotalsCard(ui, currency, convention, ::money) }
                    item { AllocationCard(ui.derived, currency, ui.rates) }
                    items(ui.derived, key = { it.holding.id }) { row ->
                        HoldingCard(
                            row = row,
                            convention = convention,
                            money = ::money,
                            onEdit = { dialog = DialogState.Edit(row) },
                            onDelete = { dialog = DialogState.Delete(row) },
                        )
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        DialogState.Add -> HoldingDialog(
            initial = null,
            onSubmit = { vm.addHolding(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is DialogState.Edit -> HoldingDialog(
            initial = d.holding.holding,
            onSubmit = { vm.updateHolding(d.holding.holding.id, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is DialogState.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete ${d.holding.holding.ticker}?") },
            text = { Text("This removes it from your portfolio and the widget.") },
            confirmButton = { TextButton(onClick = { vm.deleteHolding(d.holding.holding.id); dialog = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
        DialogState.Settings -> SettingsDialog(
            settings = ui.settings,
            canExport = ui.derived.isNotEmpty(),
            onInterval = vm::setRefreshInterval,
            onConvention = vm::setColorConvention,
            onExport = { exportLauncher.launch("portfolio.csv") },
            onAddWidget = vm::addWidgetToHomeScreen,
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun trendColor(trend: Trend, convention: ColorConvention): Color =
    colorResource(trendColorRes(trend, convention))

private fun trendOf(value: Double?): Trend = when {
    value == null -> Trend.UNKNOWN
    value > 0 -> Trend.UP
    value < 0 -> Trend.DOWN
    else -> Trend.FLAT
}

private fun arrowFor(trend: Trend) = when (trend) { Trend.UP -> "▲"; Trend.DOWN -> "▼"; else -> "–" }

@Composable
private fun appCardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No stocks yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(6.dp))
        Text(
            "Tap “Add stock” to start tracking, then add the widget to your home screen from Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TotalsCard(ui: UiState, currency: Currency, convention: ColorConvention, money: (Double?) -> String) {
    val totals = ui.totals
    val gainTrend = trendOf(totals.gainLossUsd.takeIf { totals.costBasisUsd > 0 })
    val dayTrend = trendOf(totals.dayChangeUsd)

    Card(colors = appCardColors(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Total value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(money(totals.marketValueUsd), fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total gain/loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${money(totals.gainLossUsd)} (${formatPercent(totals.gainLossPct)})",
                        color = trendColor(gainTrend, convention),
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (totals.dayChangeUsd == null) "—" else "${arrowFor(dayTrend)} ${formatPercent(totals.dayChangePct)}",
                        color = trendColor(dayTrend, convention),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AllocationCard(derived: List<DerivedHolding>, currency: Currency, rates: Map<String, Double>) {
    val slices = buildSlices(derived, chartPalette())
    if (slices.isEmpty()) return
    val total = slices.sumOf { it.value }

    Card(colors = appCardColors(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val stocks = derived.map { it.holding.ticker }.distinct().size
            DonutChart(slices, centerLabel = "$stocks ${if (stocks == 1) "stock" else "stocks"}")
            // A legend with values, because a colour alone should never be the only way to tell slices apart.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(slice.color, CircleShape))
                        Text(slice.label, Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${String.format(Locale.US, "%.1f", slice.value / total * 100)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingCard(
    row: DerivedHolding,
    convention: ColorConvention,
    money: (Double?) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val h = row.holding
    val gainTrend = trendOf(row.gainLossUsd)
    val dayTrend = trendOf(row.dayChangeUsd)
    var menuOpen by remember { mutableStateOf(false) }

    Card(colors = appCardColors(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(h.ticker, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${formatShares(h.shares)} ${if (h.shares == 1.0) "share" else "shares"} · bought at ${money(row.buyPriceUsd)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(row.marketValueUsd), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "now ${money(row.currentPriceUsd)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More options for ${h.ticker}") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (row.gainLossUsd == null) "—" else "${money(row.gainLossUsd)} (${formatPercent(row.gainLossPct)})",
                    color = trendColor(gainTrend, convention),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Today " + if (row.dayChangePct == null) "—" else "${arrowFor(dayTrend)} ${formatPercent(row.dayChangePct)}",
                    color = trendColor(dayTrend, convention),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
