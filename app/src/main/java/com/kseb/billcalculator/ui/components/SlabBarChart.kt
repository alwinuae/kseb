package com.kseb.billcalculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kseb.billcalculator.model.SlabDetail
import com.kseb.billcalculator.ui.theme.SlabColor1
import com.kseb.billcalculator.ui.theme.SlabColor2
import com.kseb.billcalculator.ui.theme.SlabColor3
import com.kseb.billcalculator.ui.theme.SlabColor4
import com.kseb.billcalculator.ui.theme.SlabColor5

private val slabColors = listOf(SlabColor1, SlabColor2, SlabColor3, SlabColor4, SlabColor5)

@Composable
fun SlabBarChart(
    slabDetails: List<SlabDetail>,
    totalUnits: Int,
    modifier: Modifier = Modifier
) {
    if (totalUnits <= 0 || slabDetails.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Stacked horizontal bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            slabDetails.forEachIndexed { index, slab ->
                if (slab.units > 0) {
                    val fraction = slab.units.toFloat() / totalUnits.toFloat()
                    val color = slabColors.getOrElse(index) { slabColors.last() }
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxHeight()
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fraction > 0.08f) {
                            Text(
                                text = "${slab.units}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            slabDetails.forEachIndexed { index, slab ->
                if (slab.units > 0) {
                    val color = slabColors.getOrElse(index) { slabColors.last() }
                    SlabLegendItem(
                        color = color,
                        label = slab.label,
                        units = slab.units
                    )
                }
            }
        }
    }
}

@Composable
private fun SlabLegendItem(
    color: Color,
    label: String,
    units: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$units units",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
