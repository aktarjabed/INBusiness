package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aktarjabed.inbusiness.data.entities.InvoiceItem

@Composable
fun InvoiceItemRow(
    item: InvoiceItem,
    onUpdate: (InvoiceItem) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            OutlinedTextField(
                value = item.itemName,
                onValueChange = { onUpdate(item.copy(itemName = it)) },
                label = { Text("Item") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = item.hsnCode,
                    onValueChange = { onUpdate(item.copy(hsnCode = it)) },
                    label = { Text("HSN") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = item.quantity.toString(),
                    onValueChange = {
                        val qty = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(quantity = qty, totalAmount = qty * item.rate))
                    },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = item.rate.toString(),
                    onValueChange = {
                        val rate = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(rate = rate, totalAmount = item.quantity * rate))
                    },
                    label = { Text("Rate") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}