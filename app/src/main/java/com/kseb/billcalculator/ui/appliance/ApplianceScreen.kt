package com.kseb.billcalculator.ui.appliance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import com.kseb.billcalculator.model.ApplianceResult
import com.kseb.billcalculator.model.BillBreakdown
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.PRESET_APPLIANCES
import com.kseb.billcalculator.ui.components.BillBreakdownCard
import com.kseb.billcalculator.ui.components.BillingCycleToggle
import com.kseb.billcalculator.ui.components.CustomRatesIndicator
import com.kseb.billcalculator.ui.components.PhaseSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCompare: (Int, BillingCycle) -> Unit,
    onExportPdf: (ApplianceResult, BillBreakdown) -> Unit,
    viewModel: ApplianceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appliance Calculator") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            CustomRatesIndicator(isCustomRates = uiState.isCustomRates)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // --- Appliance input section ---
                ApplianceInputSection(
                    inputState = uiState.inputState,
                    onNameChange = viewModel::updateName,
                    onWattageChange = viewModel::updateWattage,
                    onQuantityChange = viewModel::updateQuantity,
                    onHoursChange = viewModel::updateHoursPerDay,
                    onDaysChange = viewModel::updateDaysPerMonth,
                    onAddAppliance = viewModel::addAppliance,
                    onSelectPreset = { name, wattage -> viewModel.addPresetAppliance(name, wattage) },
                    inputError = uiState.inputErrorMessage
                )

                // --- Appliance list ---
                if (uiState.appliances.isNotEmpty()) {
                    ApplianceListSection(
                        appliances = uiState.appliances,
                        onRemove = viewModel::removeAppliance,
                        onClearAll = viewModel::clearAllAppliances
                    )
                }

                // --- Phase selector + Billing cycle ---
                Text(
                    text = "Phase Type",
                    style = MaterialTheme.typography.labelLarge
                )
                PhaseSelector(
                    selectedPhase = uiState.phase,
                    onPhaseSelected = viewModel::updatePhase
                )

                Text(
                    text = "Billing Cycle",
                    style = MaterialTheme.typography.labelLarge
                )
                BillingCycleToggle(
                    selectedCycle = uiState.cycle,
                    onCycleSelected = viewModel::updateCycle
                )

                // --- Calculate button ---
                Button(
                    onClick = viewModel::calculate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.appliances.isNotEmpty() && !uiState.isCalculating
                ) {
                    Text("CALCULATE BILL")
                }

                // --- Error message ---
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // --- Results ---
                uiState.applianceResult?.let { result ->
                    uiState.billBreakdown?.let { breakdown ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        BillBreakdownCard(breakdown = breakdown)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onExportPdf(result, breakdown) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text("Export PDF")
                            }

                            FilledTonalButton(
                                onClick = {
                                    onNavigateToCompare(result.totalUnits, uiState.cycle)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CompareArrows,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text("Compare Phases")
                            }
                        }
                    }
                }

                // Disclaimer
                Text(
                    text = "ESTIMATE ONLY - Actual bill may vary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplianceInputSection(
    inputState: ApplianceInputState,
    onNameChange: (String) -> Unit,
    onWattageChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onHoursChange: (String) -> Unit,
    onDaysChange: (String) -> Unit,
    onAddAppliance: () -> Unit,
    onSelectPreset: (String, Double) -> Unit,
    inputError: String?
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add Appliance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            // Appliance name with dropdown
            var expanded by remember { mutableStateOf(false) }
            val filteredPresets = remember(inputState.name) {
                if (inputState.name.isBlank()) {
                    PRESET_APPLIANCES
                } else {
                    PRESET_APPLIANCES.filter {
                        it.name.contains(inputState.name, ignoreCase = true)
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = inputState.name,
                    onValueChange = { value ->
                        onNameChange(value)
                        expanded = true
                    },
                    label = { Text("Appliance Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                if (filteredPresets.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(preset.name)
                                        Text(
                                            text = "${preset.defaultWattage.toInt()}W",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectPreset(preset.name, preset.defaultWattage)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Wattage
            OutlinedTextField(
                value = inputState.wattage,
                onValueChange = onWattageChange,
                label = { Text("Wattage (W)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Quantity, Hours, Days in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputState.quantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = inputState.hoursPerDay,
                    onValueChange = onHoursChange,
                    label = { Text("Hrs/Day") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = inputState.daysPerMonth,
                    onValueChange = onDaysChange,
                    label = { Text("Days/Mon") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Error message
            AnimatedVisibility(
                visible = inputError != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                inputError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Add button
            Button(
                onClick = onAddAppliance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("ADD APPLIANCE")
            }
        }
    }
}

@Composable
private fun ApplianceListSection(
    appliances: List<com.kseb.billcalculator.model.Appliance>,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appliances (${appliances.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appliance",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(2.5f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(2.5f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "kWh/mon",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Space for delete icon
                Spacer(modifier = Modifier.width(40.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

            appliances.forEach { appliance ->
                val monthlyKwh = (appliance.wattage * appliance.hoursPerDay *
                    appliance.quantity * appliance.daysPerMonth) / 1000.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2.5f)) {
                        Text(
                            text = appliance.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${appliance.wattage.toInt()}W",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${appliance.quantity}x ${appliance.hoursPerDay}h ${appliance.daysPerMonth}d",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(2.5f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.1f".format(monthlyKwh),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { onRemove(appliance.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove ${appliance.name}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Total units
            val totalKwh = appliances.sumOf { appliance ->
                (appliance.wattage * appliance.hoursPerDay *
                    appliance.quantity * appliance.daysPerMonth) / 1000.0
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val totalUnits = totalKwh.roundToInt()
                Text(
                    text = "%.1f kWh/month (~$totalUnits units)".format(totalKwh),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

