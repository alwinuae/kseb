package com.kseb.billcalculator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kseb.billcalculator.model.BillingCycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingCycleToggle(
    selectedCycle: BillingCycle,
    onCycleSelected: (BillingCycle) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        BillingCycle.entries.forEachIndexed { index, cycle ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, BillingCycle.entries.size),
                onClick = { onCycleSelected(cycle) },
                selected = selectedCycle == cycle
            ) {
                Text(cycle.displayName)
            }
        }
    }
}
