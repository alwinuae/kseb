package com.kseb.billcalculator.model

import kotlinx.serialization.Serializable

@Serializable
enum class PhaseType {
    SINGLE_PHASE,
    THREE_PHASE;

    val displayName: String
        get() = when (this) {
            SINGLE_PHASE -> "Single Phase"
            THREE_PHASE -> "Three Phase"
        }
}
