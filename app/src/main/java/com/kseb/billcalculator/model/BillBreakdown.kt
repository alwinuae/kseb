package com.kseb.billcalculator.model

import java.math.BigDecimal

data class BillBreakdown(
    val units: Int,
    val phase: PhaseType,
    val cycle: BillingCycle,
    val isTelescopicBilling: Boolean,
    val slabDetails: List<SlabDetail>,
    val totalEnergyCharge: BigDecimal,
    val fixedCharge: BigDecimal,
    val electricityDuty: BigDecimal,
    val fuelSurcharge: BigDecimal,
    val meterRent: BigDecimal,
    val gstOnMeterRent: BigDecimal,
    val totalAmount: BigDecimal
)
