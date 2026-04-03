package com.kseb.billcalculator.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

fun formatIndianCurrency(amount: BigDecimal): String {
    val formatted = amount.setScale(2, RoundingMode.HALF_UP)
    val parts = formatted.toPlainString().split(".")
    val intPart = parts[0]
    val decPart = parts.getOrElse(1) { "00" }

    val negative = intPart.startsWith("-")
    val digits = if (negative) intPart.substring(1) else intPart

    val result = if (digits.length <= 3) {
        digits
    } else {
        val last3 = digits.substring(digits.length - 3)
        val remaining = digits.substring(0, digits.length - 3)
        val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
        "$grouped,$last3"
    }

    return "\u20B9${if (negative) "-" else ""}$result.$decPart"
}
