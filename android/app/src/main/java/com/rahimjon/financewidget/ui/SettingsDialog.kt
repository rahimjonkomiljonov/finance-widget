package com.rahimjon.financewidget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rahimjon.financewidget.data.ColorConvention
import com.rahimjon.financewidget.data.Settings

private val IntervalOptions = listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h")

@Composable
fun SettingsDialog(
    settings: Settings,
    canExport: Boolean,
    onInterval: (Int) -> Unit,
    onConvention: (ColorConvention) -> Unit,
    onExport: () -> Unit,
    onAddWidget: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Widget refresh", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    IntervalOptions.forEachIndexed { index, (minutes, label) ->
                        SegmentedButton(
                            selected = settings.refreshIntervalMinutes == minutes,
                            onClick = { onInterval(minutes) },
                            shape = SegmentedButtonDefaults.itemShape(index, IntervalOptions.size),
                        ) { Text(label, maxLines = 1) }
                    }
                }
                Text(
                    "Android limits background updates to once every 15 minutes at most. " +
                        "Tap ↻ on the widget to refresh right away.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()
                Text("Price colours", style = MaterialTheme.typography.titleSmall)
                ConventionRow("Red up, blue down (Korean)", settings.colorConvention == ColorConvention.KOREAN) {
                    onConvention(ColorConvention.KOREAN)
                }
                ConventionRow("Green up, red down (Western)", settings.colorConvention == ColorConvention.WESTERN) {
                    onConvention(ColorConvention.WESTERN)
                }

                HorizontalDivider()
                OutlinedButton(onClick = onAddWidget, modifier = Modifier.fillMaxWidth()) { Text("Add widget to home screen") }
                OutlinedButton(onClick = onExport, enabled = canExport, modifier = Modifier.fillMaxWidth()) { Text("Export to CSV") }
                if (version.isNotEmpty()) {
                    Text("Version $version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ConventionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
