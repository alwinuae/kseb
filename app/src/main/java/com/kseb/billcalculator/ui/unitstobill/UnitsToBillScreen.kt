package com.kseb.billcalculator.ui.unitstobill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kseb.billcalculator.model.BillBreakdown
import com.kseb.billcalculator.ui.components.BillBreakdownCard
import com.kseb.billcalculator.ui.components.BillingCycleToggle
import com.kseb.billcalculator.ui.components.CustomRatesIndicator
import com.kseb.billcalculator.ui.components.PhaseSelector
import com.kseb.billcalculator.ui.components.SlabBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsToBillScreen(
    onNavigateToSettings: () -> Unit,
    onExportPdf: (BillBreakdown) -> Unit,
    viewModel: UnitsToBillViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val immediateState by viewModel.immediateInputState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Units to Bill") },
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
                    selectedPhase = immediateState.phase,
                    onPhaseSelected = { viewModel.updatePhase(it) }
                )

                // Billing cycle toggle
                BillingCycleToggle(
                    selectedCycle = immediateState.cycle,
                    onCycleSelected = { viewModel.updateCycle(it) }
                )

                // Units input
                OutlinedTextField(
                    value = immediateState.unitsInput,
                    onValueChange = { viewModel.updateUnits(it) },
                    label = { Text("Units Consumed") },
                    placeholder = { Text("Enter units (kWh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = uiState.errorMessage != null,
                    supportingText = if (uiState.errorMessage != null) {
                        { Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Results section
                val breakdown = uiState.breakdown
                if (breakdown != null) {
                    // Slab bar chart
                    if (breakdown.slabDetails.isNotEmpty()) {
                        SlabBarChart(
                            slabDetails = breakdown.slabDetails,
                            totalUnits = breakdown.units
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bill breakdown card
                    BillBreakdownCard(breakdown = breakdown)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Export PDF button
                    FilledTonalButton(
                        onClick = { onExportPdf(breakdown) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Export PDF")
                    }
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
