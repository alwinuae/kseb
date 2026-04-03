package com.kseb.billcalculator.ui.phasecompare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kseb.billcalculator.model.BillBreakdown
import com.kseb.billcalculator.model.SlabDetail
import com.kseb.billcalculator.ui.components.BillingCycleToggle
import com.kseb.billcalculator.ui.components.CustomRatesIndicator
import com.kseb.billcalculator.ui.components.formatIndianCurrency
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseCompareScreen(
    onNavigateToSettings: () -> Unit,
    onExportPdf: (BillBreakdown, BillBreakdown) -> Unit,
    viewModel: PhaseCompareViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val immediateState by viewModel.immediateInputState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phase Comparison") },
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

                // Billing cycle toggle
                Text(
                    text = "Billing Cycle",
                    style = MaterialTheme.typography.labelLarge
                )
                BillingCycleToggle(
                    selectedCycle = immediateState.cycle,
                    onCycleSelected = viewModel::updateCycle
                )

                // Units input
                OutlinedTextField(
                    value = immediateState.unitsInput,
                    onValueChange = viewModel::updateUnits,
                    label = { Text("Units Consumed") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text("Enter monthly units for comparison")
                    }
                )

                // Error message
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

                // Side-by-side comparison
                val singleBreakdown = uiState.singlePhaseBreakdown
                val threeBreakdown = uiState.threePhaseBreakdown

                if (singleBreakdown != null && threeBreakdown != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Comparison header
                    Text(
                        text = "Comparison for ${singleBreakdown.units} units (${singleBreakdown.cycle.displayName})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Side-by-side cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhaseBreakdownCard(
                            title = "Single Phase",
                            breakdown = singleBreakdown,
                            modifier = Modifier.weight(1f)
                        )
                        PhaseBreakdownCard(
                            title = "Three Phase",
                            breakdown = threeBreakdown,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Recommendation
                    uiState.difference?.let { diff ->
                        RecommendationCard(
                            difference = diff,
                            units = singleBreakdown.units,
                            cycle = singleBreakdown.cycle.displayName
                        )
                    }

                    // Export PDF button
                    OutlinedButton(
                        onClick = { onExportPdf(singleBreakdown, threeBreakdown) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Export PDF")
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

@Composable
private fun PhaseBreakdownCard(
    title: String,
    breakdown: BillBreakdown,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Billing type
            Text(
                text = if (breakdown.isTelescopicBilling) "Telescopic" else "Non-Telescopic",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Slab details
            if (breakdown.slabDetails.isNotEmpty()) {
                Text(
                    text = "Slab Details",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                breakdown.slabDetails.forEach { slab ->
                    CompactSlabRow(slab)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            }

            // Charge components
            CompactChargeRow("Energy", breakdown.totalEnergyCharge)
            CompactChargeRow("Fixed", breakdown.fixedCharge)
            CompactChargeRow("Duty", breakdown.electricityDuty)
            CompactChargeRow("Fuel Surcharge", breakdown.fuelSurcharge)
            CompactChargeRow("Meter Rent", breakdown.meterRent)
            CompactChargeRow("GST", breakdown.gstOnMeterRent)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = formatIndianCurrency(breakdown.totalAmount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CompactSlabRow(slab: SlabDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${slab.label} (${slab.units}u)",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatIndianCurrency(slab.charge),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CompactChargeRow(label: String, amount: BigDecimal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatIndianCurrency(amount),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun RecommendationCard(
    difference: BigDecimal,
    units: Int,
    cycle: String
) {
    val isThreePhaseMore = difference > BigDecimal.ZERO
    val absDifference = difference.abs()

    val recommendationText = when {
        difference.compareTo(BigDecimal.ZERO) == 0 ->
            "Both phases cost the same for $units units ($cycle)"
        isThreePhaseMore ->
            "Single Phase saves ${formatIndianCurrency(absDifference)} per $cycle for $units units usage"
        else ->
            "Three Phase saves ${formatIndianCurrency(absDifference)} per $cycle for $units units usage"
    }

    val containerColor = when {
        difference.compareTo(BigDecimal.ZERO) == 0 -> MaterialTheme.colorScheme.surfaceVariant
        isThreePhaseMore -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when {
        difference.compareTo(BigDecimal.ZERO) == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        isThreePhaseMore -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Recommendation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = recommendationText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            if (difference.compareTo(BigDecimal.ZERO) != 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Single Phase",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Difference",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor
                        )
                        Text(
                            text = formatIndianCurrency(absDifference),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Three Phase",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
