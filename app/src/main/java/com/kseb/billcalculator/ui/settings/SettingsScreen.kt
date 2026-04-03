package com.kseb.billcalculator.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Tariff Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info text
            Text(
                text = "Default rates: KSEB Domestic LT-1A, effective 05.12.2024",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error message
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Saved indicator
            AnimatedVisibility(
                visible = state.isSaved,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Telescopic Slabs
            val telescopicLabels = listOf("0-50", "51-100", "101-150", "151-200", "201-250")
            SectionCard(title = "Telescopic Slabs (0-250 units)") {
                telescopicLabels.forEachIndexed { index, label ->
                    RateTextField(
                        label = "$label units",
                        value = state.telescopicRates[index],
                        onValueChange = { viewModel.updateTelescopicRate(index, it) },
                        suffix = "/unit"
                    )
                }
            }

            // Non-Telescopic Slabs
            val nonTelescopicLabels = listOf("251-300", "301-350", "351-400", "401-500", "501+")
            SectionCard(title = "Non-Telescopic Slabs (251+ units)") {
                nonTelescopicLabels.forEachIndexed { index, label ->
                    RateTextField(
                        label = "$label units",
                        value = state.nonTelescopicRates[index],
                        onValueChange = { viewModel.updateNonTelescopicRate(index, it) },
                        suffix = "/unit"
                    )
                }
            }

            // Fixed Charges - Single Phase
            val fixedChargeLabels = listOf("0-100", "101-200", "201-300", "301-500", "501+")
            SectionCard(title = "Fixed Charges - Single Phase") {
                fixedChargeLabels.forEachIndexed { index, label ->
                    RateTextField(
                        label = "$label units",
                        value = state.fixedChargesSingle[index],
                        onValueChange = { viewModel.updateFixedChargeSingle(index, it) },
                        suffix = "/month"
                    )
                }
            }

            // Fixed Charges - Three Phase
            SectionCard(title = "Fixed Charges - Three Phase") {
                fixedChargeLabels.forEachIndexed { index, label ->
                    RateTextField(
                        label = "$label units",
                        value = state.fixedChargesThree[index],
                        onValueChange = { viewModel.updateFixedChargeThree(index, it) },
                        suffix = "/month"
                    )
                }
            }

            // Additional Charges
            SectionCard(title = "Additional Charges") {
                RateTextField(
                    label = "Electricity Duty",
                    value = state.electricityDutyPercent,
                    onValueChange = { viewModel.updateElectricityDuty(it) },
                    suffix = "%"
                )
                RateTextField(
                    label = "Fuel Surcharge",
                    value = state.fuelSurchargePerUnit,
                    onValueChange = { viewModel.updateFuelSurcharge(it) },
                    suffix = "/unit"
                )
                RateTextField(
                    label = "Meter Rent (Single Phase)",
                    value = state.meterRentSingle,
                    onValueChange = { viewModel.updateMeterRentSingle(it) },
                    suffix = "/month"
                )
                RateTextField(
                    label = "Meter Rent (Three Phase)",
                    value = state.meterRentThree,
                    onValueChange = { viewModel.updateMeterRentThree(it) },
                    suffix = "/month"
                )
                RateTextField(
                    label = "GST on Meter Rent",
                    value = state.gstPercent,
                    onValueChange = { viewModel.updateGstPercent(it) },
                    suffix = "%"
                )
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset to Defaults")
                }
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Defaults") },
            text = { Text("This will reset all tariff rates to the official KSEB LT-1A rates. Any custom values will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun RateTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
        isError = value.toDoubleOrNull() == null && value.isNotEmpty()
    )
}
