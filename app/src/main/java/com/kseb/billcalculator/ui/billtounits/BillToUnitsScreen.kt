package com.kseb.billcalculator.ui.billtounits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.GapOption
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.ReverseResult
import com.kseb.billcalculator.ui.components.BillBreakdownCard
import com.kseb.billcalculator.ui.components.BillingCycleToggle
import com.kseb.billcalculator.ui.components.CustomRatesIndicator
import com.kseb.billcalculator.ui.components.PhaseSelector
import com.kseb.billcalculator.ui.components.formatIndianCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillToUnitsScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCalculator: (Int, PhaseType, BillingCycle) -> Unit,
    onExportPdf: (ReverseResult) -> Unit,
    viewModel: BillToUnitsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill to Units") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
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
        ) {
            CustomRatesIndicator(isCustomRates = uiState.isCustomRates)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Phase selector
                PhaseSelector(
                    selectedPhase = uiState.phase,
                    onPhaseSelected = { viewModel.updatePhase(it) }
                )

                // Billing cycle toggle
                BillingCycleToggle(
                    selectedCycle = uiState.cycle,
                    onCycleSelected = { viewModel.updateCycle(it) }
                )

                // Bill amount input
                OutlinedTextField(
                    value = uiState.billAmountInput,
                    onValueChange = { viewModel.updateBillAmount(it) },
                    label = { Text("Bill Amount") },
                    placeholder = { Text("Enter bill amount") },
                    prefix = { Text("\u20B9") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = uiState.errorMessage != null,
                    supportingText = if (uiState.errorMessage != null) {
                        { Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Calculate button
                Button(
                    onClick = { viewModel.calculate() },
                    enabled = !uiState.isCalculating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isCalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("CALCULATE UNITS")
                }

                // Results
                when (val result = uiState.result) {
                    is ReverseResult.Match -> {
                        MatchResultSection(
                            result = result,
                            phase = uiState.phase,
                            cycle = uiState.cycle,
                            inputAmount = uiState.billAmountInput,
                            onNavigateToCalculator = onNavigateToCalculator,
                            onExportPdf = { onExportPdf(result) }
                        )
                    }
                    is ReverseResult.Gap -> {
                        GapResultSection(
                            result = result,
                            phase = uiState.phase,
                            cycle = uiState.cycle,
                            onNavigateToCalculator = onNavigateToCalculator
                        )
                    }
                    is ReverseResult.BelowMinimum -> {
                        BelowMinimumSection(result = result)
                    }
                    null -> { /* No result yet */ }
                }

                // Disclaimer
                Text(
                    text = "ESTIMATE ONLY - Not an official KSEB bill",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MatchResultSection(
    result: ReverseResult.Match,
    phase: PhaseType,
    cycle: BillingCycle,
    inputAmount: String,
    onNavigateToCalculator: (Int, PhaseType, BillingCycle) -> Unit,
    onExportPdf: () -> Unit
) {
    // Estimated units card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Estimated Units",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${result.units} kWh",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (result.breakdown.isTelescopicBilling) "Telescopic Billing" else "Non-Telescopic Billing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    // Verification section
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Verification",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your input",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\u20B9$inputAmount",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Calculated bill for ${result.units} units",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatIndianCurrency(result.breakdown.totalAmount),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (result.difference.signum() != 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Difference",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatIndianCurrency(result.difference),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (result.note != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Full breakdown
    BillBreakdownCard(breakdown = result.breakdown)

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onNavigateToCalculator(result.units, phase, cycle) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp)
            )
            Text("View in Calculator")
        }

        FilledTonalButton(
            onClick = onExportPdf,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp)
            )
            Text("Export PDF")
        }
    }
}

@Composable
private fun GapResultSection(
    result: ReverseResult.Gap,
    phase: PhaseType,
    cycle: BillingCycle,
    onNavigateToCalculator: (Int, PhaseType, BillingCycle) -> Unit
) {
    // Warning card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "No Exact Match",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    // Nearest options
    Text(
        text = "Nearest Options",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )

    if (result.lowerOption != null) {
        GapOptionCard(
            label = "Lower",
            option = result.lowerOption,
            onViewBreakdown = {
                onNavigateToCalculator(result.lowerOption.units, phase, cycle)
            }
        )
    }

    if (result.upperOption != null) {
        GapOptionCard(
            label = "Upper",
            option = result.upperOption,
            onViewBreakdown = {
                onNavigateToCalculator(result.upperOption.units, phase, cycle)
            }
        )
    }
}

@Composable
private fun GapOptionCard(
    label: String,
    option: GapOption,
    onViewBreakdown: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$label Option",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${option.units} units",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = formatIndianCurrency(option.billAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onViewBreakdown,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Breakdown")
            }
        }
    }
}

@Composable
private fun BelowMinimumSection(result: ReverseResult.BelowMinimum) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Below Minimum Bill",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "The entered amount is below the minimum possible bill of ${formatIndianCurrency(result.minimumBill)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Minimum Bill Breakdown",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )

    BillBreakdownCard(breakdown = result.minimumBreakdown)
}
