package com.kseb.billcalculator.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SlabRate(
    val lowerLimit: Int,
    val upperLimit: Int,
    val rate: Double
) {
    val label: String get() = if (upperLimit == Int.MAX_VALUE) "${lowerLimit}+" else "$lowerLimit–$upperLimit"
    val width: Int get() = if (upperLimit == Int.MAX_VALUE) Int.MAX_VALUE else upperLimit - lowerLimit + 1
    val rateBD: BigDecimal get() = BigDecimal.valueOf(rate)
}

@Serializable
data class FixedChargeRange(
    val lowerLimit: Int,
    val upperLimit: Int,
    val singlePhaseCharge: Double,
    val threePhaseCharge: Double
) {
    fun getCharge(phase: PhaseType): BigDecimal = when (phase) {
        PhaseType.SINGLE_PHASE -> BigDecimal.valueOf(singlePhaseCharge)
        PhaseType.THREE_PHASE -> BigDecimal.valueOf(threePhaseCharge)
    }
}

@Serializable
data class TariffConfig(
    val telescopicSlabs: List<SlabRate> = DEFAULT_TELESCOPIC_SLABS,
    val nonTelescopicSlabs: List<SlabRate> = DEFAULT_NON_TELESCOPIC_SLABS,
    val fixedChargeRanges: List<FixedChargeRange> = DEFAULT_FIXED_CHARGES,
    val electricityDutyPercent: Double = 10.0,
    val fuelSurchargePerUnit: Double = 0.10,
    val meterRentSinglePhase: Double = 6.0,
    val meterRentThreePhase: Double = 15.0,
    val gstOnMeterRentPercent: Double = 18.0,
    val telescopicLimit: Int = 250
) {
    fun getNonTelescopicRate(units: Int): BigDecimal {
        val slab = nonTelescopicSlabs.find { units in it.lowerLimit..it.upperLimit }
            ?: nonTelescopicSlabs.last()
        return slab.rateBD
    }

    fun getFixedCharge(units: Int, phase: PhaseType): BigDecimal {
        val range = fixedChargeRanges.find { units in it.lowerLimit..it.upperLimit }
            ?: fixedChargeRanges.last()
        return range.getCharge(phase)
    }

    fun getMeterRent(phase: PhaseType): BigDecimal = when (phase) {
        PhaseType.SINGLE_PHASE -> BigDecimal.valueOf(meterRentSinglePhase)
        PhaseType.THREE_PHASE -> BigDecimal.valueOf(meterRentThreePhase)
    }

    fun isDefault(): Boolean = this == DEFAULT

    companion object {
        val DEFAULT_TELESCOPIC_SLABS = listOf(
            SlabRate(1, 50, 3.35),
            SlabRate(51, 100, 4.25),
            SlabRate(101, 150, 5.35),
            SlabRate(151, 200, 7.20),
            SlabRate(201, 250, 8.50),
        )

        val DEFAULT_NON_TELESCOPIC_SLABS = listOf(
            SlabRate(251, 300, 6.75),
            SlabRate(301, 350, 7.60),
            SlabRate(351, 400, 7.95),
            SlabRate(401, 500, 8.25),
            SlabRate(501, Int.MAX_VALUE, 9.20),
        )

        val DEFAULT_FIXED_CHARGES = listOf(
            FixedChargeRange(0, 100, 35.0, 85.0),
            FixedChargeRange(101, 200, 55.0, 130.0),
            FixedChargeRange(201, 300, 75.0, 160.0),
            FixedChargeRange(301, 500, 100.0, 200.0),
            FixedChargeRange(501, Int.MAX_VALUE, 125.0, 250.0),
        )

        val DEFAULT = TariffConfig()
    }
}
