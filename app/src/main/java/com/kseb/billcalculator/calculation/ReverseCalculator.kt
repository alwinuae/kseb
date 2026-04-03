package com.kseb.billcalculator.calculation

import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.GapOption
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.ReverseResult
import com.kseb.billcalculator.model.TariffConfig
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt

object ReverseCalculator {

    fun reverseBill(
        totalAmount: BigDecimal,
        phase: PhaseType,
        cycle: BillingCycle,
        tariff: TariffConfig
    ): ReverseResult {
        if (totalAmount <= BigDecimal.ZERO) {
            val zeroBill = BillCalculator.calculateBill(0, phase, cycle, tariff)
            return ReverseResult.BelowMinimum(zeroBill.totalAmount, zeroBill)
        }

        // Handle bimonthly: solve for monthly amount, then double units
        if (cycle == BillingCycle.BIMONTHLY) {
            val monthlyAmount = totalAmount.divide(BigDecimal(2), 10, RoundingMode.HALF_UP)
            return when (val monthlyResult = reverseBill(monthlyAmount, phase, BillingCycle.MONTHLY, tariff)) {
                is ReverseResult.Match -> {
                    val bimonthlyUnits = monthlyResult.units * 2
                    val bimonthlyBreakdown = BillCalculator.calculateBill(bimonthlyUnits, phase, cycle, tariff)
                    val diff = (bimonthlyBreakdown.totalAmount - totalAmount).abs()
                    ReverseResult.Match(
                        units = bimonthlyUnits,
                        breakdown = bimonthlyBreakdown,
                        difference = diff,
                        note = if (diff > BigDecimal("0.01")) "Rounding difference of ₹${diff.scale2()}" else null
                    )
                }
                is ReverseResult.Gap -> {
                    val lowerOption = monthlyResult.lowerOption?.let {
                        val bUnits = it.units * 2
                        val bBreakdown = BillCalculator.calculateBill(bUnits, phase, cycle, tariff)
                        GapOption(bUnits, bBreakdown.totalAmount, bBreakdown)
                    }
                    val upperOption = monthlyResult.upperOption?.let {
                        val bUnits = it.units * 2
                        val bBreakdown = BillCalculator.calculateBill(bUnits, phase, cycle, tariff)
                        GapOption(bUnits, bBreakdown.totalAmount, bBreakdown)
                    }
                    ReverseResult.Gap(monthlyResult.message, lowerOption, upperOption)
                }
                is ReverseResult.BelowMinimum -> {
                    val biBreakdown = BillCalculator.calculateBill(0, phase, cycle, tariff)
                    ReverseResult.BelowMinimum(biBreakdown.totalAmount, biBreakdown)
                }
            }
        }

        // Check if below minimum bill (0 units)
        val zeroBill = BillCalculator.calculateBill(0, phase, BillingCycle.MONTHLY, tariff)
        if (totalAmount < zeroBill.totalAmount) {
            return ReverseResult.BelowMinimum(zeroBill.totalAmount, zeroBill)
        }

        val candidates = mutableListOf<Candidate>()
        val dutyMultiplier = 1.0 + tariff.electricityDutyPercent / 100.0
        val fuelRate = tariff.fuelSurchargePerUnit

        // --- TELESCOPIC RANGE (0-250 units) ---
        for (fixedRange in tariff.fixedChargeRanges) {
            val fixedCharge = when (phase) {
                PhaseType.SINGLE_PHASE -> fixedRange.singlePhaseCharge
                PhaseType.THREE_PHASE -> fixedRange.threePhaseCharge
            }
            val meterRent = when (phase) {
                PhaseType.SINGLE_PHASE -> tariff.meterRentSinglePhase
                PhaseType.THREE_PHASE -> tariff.meterRentThreePhase
            }
            val gstOnMeter = meterRent * tariff.gstOnMeterRentPercent / 100.0
            val constants = fixedCharge + meterRent + gstOnMeter

            // Remaining amount for energy + duty + fuel surcharge
            val remainingAmount = totalAmount.toDouble() - constants

            if (remainingAmount < 0) continue

            // Try to solve within each telescopic slab
            var baseEnergy = 0.0
            var baseUnits = 0
            for (slab in tariff.telescopicSlabs) {
                val r = slab.rate
                val slabLower = if (slab.lowerLimit == 0) 0 else baseUnits
                val slabWidth = slab.width

                // Energy for units u in this slab:
                // energyCharge = baseEnergy + (u - baseUnits) * r
                // total = energyCharge * dutyMultiplier + u * fuelRate + constants
                // Solve: u = (totalAmount - baseEnergy * dutyMultiplier - constants + baseUnits * r * dutyMultiplier) /
                //             (r * dutyMultiplier + fuelRate)
                val numerator = totalAmount.toDouble() - baseEnergy * dutyMultiplier - constants + baseUnits * r * dutyMultiplier
                val denominator = r * dutyMultiplier + fuelRate

                if (denominator > 0) {
                    val u = numerator / denominator
                    // Check both floor and ceil
                    for (candidate in listOf(u.toInt(), u.toInt() + 1)) {
                        if (candidate < 0) continue
                        if (candidate > tariff.telescopicLimit) continue
                        // Check if candidate is within this slab range
                        if (candidate < slabLower) continue
                        if (candidate > baseUnits + slabWidth) continue
                        // Check if candidate falls within this fixed charge range
                        if (candidate < fixedRange.lowerLimit || candidate > fixedRange.upperLimit) continue

                        val bill = BillCalculator.calculateBill(candidate, phase, BillingCycle.MONTHLY, tariff)
                        val diff = (bill.totalAmount - totalAmount).abs()
                        candidates.add(Candidate(candidate, bill.totalAmount, diff, bill))
                    }
                }

                baseEnergy += slabWidth * r
                baseUnits += slabWidth
            }
        }

        // --- NON-TELESCOPIC RANGE (251+ units) ---
        for (band in tariff.nonTelescopicSlabs) {
            for (fixedRange in tariff.fixedChargeRanges) {
                val fixedCharge = when (phase) {
                    PhaseType.SINGLE_PHASE -> fixedRange.singlePhaseCharge
                    PhaseType.THREE_PHASE -> fixedRange.threePhaseCharge
                }
                val meterRent = when (phase) {
                    PhaseType.SINGLE_PHASE -> tariff.meterRentSinglePhase
                    PhaseType.THREE_PHASE -> tariff.meterRentThreePhase
                }
                val gstOnMeter = meterRent * tariff.gstOnMeterRentPercent / 100.0
                val constants = fixedCharge + meterRent + gstOnMeter

                val f = band.rate
                // total = u * f * dutyMultiplier + u * fuelRate + constants
                // u = (totalAmount - constants) / (f * dutyMultiplier + fuelRate)
                val denominator = f * dutyMultiplier + fuelRate
                if (denominator <= 0) continue

                val u = (totalAmount.toDouble() - constants) / denominator

                for (candidate in listOf(u.toInt(), u.toInt() + 1)) {
                    if (candidate < band.lowerLimit) continue
                    if (candidate > band.upperLimit) continue
                    if (candidate < fixedRange.lowerLimit || candidate > fixedRange.upperLimit) continue

                    val bill = BillCalculator.calculateBill(candidate, phase, BillingCycle.MONTHLY, tariff)
                    val diff = (bill.totalAmount - totalAmount).abs()
                    candidates.add(Candidate(candidate, bill.totalAmount, diff, bill))
                }
            }
        }

        if (candidates.isEmpty()) {
            return findGapResult(totalAmount, phase, tariff)
        }

        // Find best candidate (smallest difference)
        val best = candidates.minByOrNull { it.difference }!!

        // Check if it's close enough (within rounding tolerance)
        return if (best.difference <= BigDecimal("0.02")) {
            ReverseResult.Match(
                units = best.units,
                breakdown = best.breakdown,
                difference = best.difference,
                note = if (best.difference > BigDecimal("0.00")) "Rounding difference of ₹${best.difference.scale2()}" else null
            )
        } else {
            // Amount falls in a gap
            findGapResult(totalAmount, phase, tariff)
        }
    }

    private fun findGapResult(
        totalAmount: BigDecimal,
        phase: PhaseType,
        tariff: TariffConfig
    ): ReverseResult {
        // Search for nearest valid bills below and above
        var lowerOption: GapOption? = null
        var upperOption: GapOption? = null

        // Search downward from a reasonable upper bound
        val maxUnits = 2000
        for (u in maxUnits downTo 0) {
            val bill = BillCalculator.calculateBill(u, phase, BillingCycle.MONTHLY, tariff)
            if (bill.totalAmount <= totalAmount) {
                lowerOption = GapOption(u, bill.totalAmount, bill)
                // Check one above
                if (u < maxUnits) {
                    val billAbove = BillCalculator.calculateBill(u + 1, phase, BillingCycle.MONTHLY, tariff)
                    if (billAbove.totalAmount >= totalAmount) {
                        upperOption = GapOption(u + 1, billAbove.totalAmount, billAbove)
                    }
                }
                break
            }
        }

        // If we didn't find an upper option, search upward
        if (upperOption == null) {
            for (u in 0..maxUnits) {
                val bill = BillCalculator.calculateBill(u, phase, BillingCycle.MONTHLY, tariff)
                if (bill.totalAmount >= totalAmount) {
                    upperOption = GapOption(u, bill.totalAmount, bill)
                    break
                }
            }
        }

        return ReverseResult.Gap(
            message = "No exact unit count produces this bill amount.",
            lowerOption = lowerOption,
            upperOption = upperOption
        )
    }

    private data class Candidate(
        val units: Int,
        val billAmount: BigDecimal,
        val difference: BigDecimal,
        val breakdown: com.kseb.billcalculator.model.BillBreakdown
    )

    private fun BigDecimal.scale2(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
