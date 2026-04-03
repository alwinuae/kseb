package com.kseb.billcalculator.model

import java.math.BigDecimal

sealed class ReverseResult {
    data class Match(
        val units: Int,
        val breakdown: BillBreakdown,
        val difference: BigDecimal,
        val note: String? = null
    ) : ReverseResult()

    data class Gap(
        val message: String,
        val lowerOption: GapOption?,
        val upperOption: GapOption?
    ) : ReverseResult()

    data class BelowMinimum(
        val minimumBill: BigDecimal,
        val minimumBreakdown: BillBreakdown
    ) : ReverseResult()
}

data class GapOption(
    val units: Int,
    val billAmount: BigDecimal,
    val breakdown: BillBreakdown
)
