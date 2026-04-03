package com.kseb.billcalculator.model

import kotlinx.serialization.Serializable

@Serializable
enum class BillingCycle {
    MONTHLY,
    BIMONTHLY;

    val displayName: String
        get() = when (this) {
            MONTHLY -> "Monthly"
            BIMONTHLY -> "Bimonthly"
        }
}
