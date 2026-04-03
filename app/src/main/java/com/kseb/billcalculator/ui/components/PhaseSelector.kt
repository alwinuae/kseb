package com.kseb.billcalculator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kseb.billcalculator.model.PhaseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseSelector(
    selectedPhase: PhaseType,
    onPhaseSelected: (PhaseType) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        PhaseType.entries.forEachIndexed { index, phase ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, PhaseType.entries.size),
                onClick = { onPhaseSelected(phase) },
                selected = selectedPhase == phase
            ) {
                Text(phase.displayName)
            }
        }
    }
}
