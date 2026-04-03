package com.kseb.billcalculator.calculation

import com.kseb.billcalculator.model.Appliance
import com.kseb.billcalculator.model.ApplianceItem
import com.kseb.billcalculator.model.ApplianceResult
import kotlin.math.roundToInt

object ApplianceCalculator {

    fun calculateMonthlyUnits(appliances: List<Appliance>): ApplianceResult {
        val items = appliances.map { appliance ->
            val monthlyKwh = (appliance.wattage * appliance.hoursPerDay *
                appliance.quantity * appliance.daysPerMonth) / 1000.0
            ApplianceItem(appliance, monthlyKwh)
        }
        val totalUnits = items.sumOf { it.monthlyKwh }.roundToInt()
        return ApplianceResult(items, totalUnits)
    }
}
