package com.kseb.billcalculator.model

import java.math.BigDecimal

data class SlabDetail(
    val label: String,
    val rate: BigDecimal,
    val units: Int,
    val charge: BigDecimal
)
