package com.rahimjon.financewidget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rahimjon.financewidget.data.Currency
import com.rahimjon.financewidget.data.Holding
import com.rahimjon.financewidget.data.HoldingInput
import com.rahimjon.financewidget.data.formatPlain
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingDialog(initial: Holding?, onSubmit: (HoldingInput) -> Unit, onDismiss: () -> Unit) {
    var ticker by remember { mutableStateOf(initial?.ticker.orEmpty()) }
    var shares by remember { mutableStateOf(initial?.let { formatPlain(it.shares) }.orEmpty()) }
    var buyPrice by remember { mutableStateOf(initial?.let { formatPlain(it.buyPrice) }.orEmpty()) }
    var buyCurrency by remember { mutableStateOf(initial?.buyPriceCurrency ?: Currency.USD) }
    var buyDate by remember { mutableStateOf(initial?.buyDate) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun submit() {
        val sharesNum = shares.toDoubleOrNull()
        val priceNum = buyPrice.toDoubleOrNull()
        error = when {
            ticker.isBlank() -> "Ticker is required."
            sharesNum == null || sharesNum <= 0 -> "Shares must be a positive number."
            priceNum == null || priceNum <= 0 -> "Buy price must be a positive number."
            else -> null
        }
        if (error == null && sharesNum != null && priceNum != null) {
            onSubmit(HoldingInput(ticker.trim(), sharesNum, priceNum, buyCurrency, buyDate, notes.trim().ifEmpty { null }))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add stock" else "Edit stock") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it },
                    label = { Text("Ticker") },
                    placeholder = { Text("AAPL, 005930.KS") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = shares,
                    onValueChange = { shares = it },
                    label = { Text("Number of shares") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = buyPrice,
                    onValueChange = { buyPrice = it },
                    label = { Text("Buying price per share") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Bought in won or dollars: the price is stored as entered and converted for maths.
                Text(
                    "Price is in",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Currency.entries.forEachIndexed { index, currency ->
                        SegmentedButton(
                            selected = buyCurrency == currency,
                            onClick = { buyCurrency = currency },
                            shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size),
                        ) { Text(currency.name) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(buyDate?.let { "Bought $it" } ?: "Buy date (optional)")
                    }
                    if (buyDate != null) TextButton(onClick = { buyDate = null }) { Text("Clear") }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(if (initial == null) "Add" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = buyDate?.let { runCatching { java.time.LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { buyDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}
