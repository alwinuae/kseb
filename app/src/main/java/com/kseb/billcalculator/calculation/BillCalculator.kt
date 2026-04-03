package com.kseb.billcalculator.calculation

import com.kseb.billcalculator.model.BillBreakdown
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.SlabDetail
import com.kseb.billcalculator.model.TariffConfig
import java.math.BigDecimal
import java.math.RoundingMode

object BillCalculator {

    fun calculateBill(
        units: Int,
        phase: PhaseType,
        cycle: BillingCycle,
        tariff: TariffConfig
    ): BillBreakdown {
        require(units >= 0) { "Units cannot be negative" }

        // Handle bimonthly: divide by 2, calculate monthly, multiply by 2
        if (cycle == BillingCycle.BIMONTHLY) {
            val monthlyUnits = units / 2 // floor division (integer division in Kotlin)
            val monthlyBill = calculateBill(monthlyUnits, phase, BillingCycle.MONTHLY, tariff)
            return BillBreakdown(
                units = units,
                phase = phase,
                cycle = cycle,
                isTelescopicBilling = monthlyBill.isTelescopicBilling,
                slabDetails = monthlyBill.slabDetails,
                totalEnergyCharge = (monthlyBill.totalEnergyCharge * TWO).scale2(),
                fixedCharge = (monthlyBill.fixedCharge * TWO).scale2(),
                electricityDuty = (monthlyBill.electricityDuty * TWO).scale2(),
                fuelSurcharge = (monthlyBill.fuelSurcharge * TWO).scale2(),
                meterRent = (monthlyBill.meterRent * TWO).scale2(),
                gstOnMeterRent = (monthlyBill.gstOnMeterRent * TWO).scale2(),
                totalAmount = (monthlyBill.totalAmount * TWO).scale2()
            )
        }

        // Monthly calculation
        val slabDetails = mutableListOf<SlabDetail>()
        val totalEnergyCharge: BigDecimal
        val isTelescopicBilling: Boolean

        if (units <= tariff.telescopicLimit) {
            // TELESCOPIC: each slab charged at its own rate
            isTelescopicBilling = true
            var remaining = units
            for (slab in tariff.telescopicSlabs) {
                if (remaining <= 0) break
                val unitsInSlab = minOf(remaining, slab.width)
                val charge = (BigDecimal(unitsInSlab) * slab.rateBD).scale2()
                slabDetails.add(SlabDetail(slab.label, slab.rateBD, unitsInSlab, charge))
                remaining -= unitsInSlab
            }
            totalEnergyCharge = slabDetails.sumOf { it.charge }
        } else {
            // NON-TELESCOPIC: flat rate applied to ALL units
            isTelescopicBilling = false
            val flatRate = tariff.getNonTelescopicRate(units)
            totalEnergyCharge = (BigDecimal(units) * flatRate).scale2()
            slabDetails.add(
                SlabDetail(
                    "All $units units @ ₹${flatRate.stripTrailingZeros().toPlainString()}/unit",
                    flatRate,
                    units,
                    totalEnergyCharge
                )
            )
        }

        // Fixed charge
        val fixedCharge = tariff.getFixedCharge(units, phase)

        // Additional charges
        val electricityDuty = (totalEnergyCharge * BigDecimal.valueOf(tariff.electricityDutyPercent)
            / HUNDRED).scale2()
        val fuelSurcharge = (BigDecimal(units) * BigDecimal.valueOf(tariff.fuelSurchargePerUnit)).scale2()
        val meterRent = tariff.getMeterRent(phase)
        val gstOnMeterRent = (meterRent * BigDecimal.valueOf(tariff.gstOnMeterRentPercent)
            / HUNDRED).scale2()

        val totalAmount = (totalEnergyCharge + fixedCharge + electricityDuty +
            fuelSurcharge + meterRent + gstOnMeterRent).scale2()

        return BillBreakdown(
            units = units,
            phase = phase,
            cycle = cycle,
            isTelescopicBilling = isTelescopicBilling,
            slabDetails = slabDetails,
            totalEnergyCharge = totalEnergyCharge,
            fixedCharge = fixedCharge,
            electricityDuty = electricityDuty,
            fuelSurcharge = fuelSurcharge,
            meterRent = meterRent,
            gstOnMeterRent = gstOnMeterRent,
            totalAmount = totalAmount
        )
    }

    private val TWO = BigDecimal(2)
    private val HUNDRED = BigDecimal(100)

    private fun BigDecimal.scale2(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

    private fun List<SlabDetail>.sumOf(selector: (SlabDetail) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }
}
