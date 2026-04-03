package com.kseb.billcalculator.ui.phasecompare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kseb.billcalculator.calculation.BillCalculator
import com.kseb.billcalculator.data.TariffPreferences
import com.kseb.billcalculator.model.BillBreakdown
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.TariffConfig
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

data class PhaseCompareUiState(
    val unitsInput: String = "",
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val singlePhaseBreakdown: BillBreakdown? = null,
    val threePhaseBreakdown: BillBreakdown? = null,
    val difference: BigDecimal? = null,
    val errorMessage: String? = null,
    val isCustomRates: Boolean = false
)

@OptIn(FlowPreview::class)
class PhaseCompareViewModel(application: Application) : AndroidViewModel(application) {

    private val tariffPreferences = TariffPreferences(application)

    private val _unitsInput = MutableStateFlow("")
    private val _cycle = MutableStateFlow(BillingCycle.MONTHLY)

    private val tariffConfig: StateFlow<TariffConfig> = tariffPreferences.tariffConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, TariffConfig.DEFAULT)

    private val debouncedUnits = _unitsInput.debounce(300)

    val uiState: StateFlow<PhaseCompareUiState> = combine(
        debouncedUnits,
        _cycle,
        tariffConfig
    ) { units, cycle, tariff ->
        calculateState(units, cycle, tariff)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PhaseCompareUiState()
    )

    private val _immediateState = MutableStateFlow(PhaseCompareUiState())
    val immediateInputState: StateFlow<PhaseCompareUiState> = _immediateState.asStateFlow()

    fun updateUnits(input: String) {
        _unitsInput.value = input
        _immediateState.value = _immediateState.value.copy(unitsInput = input)
    }

    fun updateCycle(cycle: BillingCycle) {
        _cycle.value = cycle
        _immediateState.value = _immediateState.value.copy(cycle = cycle)
    }

    private fun calculateState(
        unitsInput: String,
        cycle: BillingCycle,
        tariff: TariffConfig
    ): PhaseCompareUiState {
        val isCustom = !tariff.isDefault()

        if (unitsInput.isBlank()) {
            return PhaseCompareUiState(
                unitsInput = unitsInput,
                cycle = cycle,
                isCustomRates = isCustom
            )
        }

        val units = unitsInput.toIntOrNull()
        if (units == null || units < 0 || units > 10000) {
            return PhaseCompareUiState(
                unitsInput = unitsInput,
                cycle = cycle,
                errorMessage = when {
                    units == null -> "Please enter a valid number"
                    units < 0 -> "Units cannot be negative"
                    else -> "Units must be 10,000 or less"
                },
                isCustomRates = isCustom
            )
        }

        val singlePhase = BillCalculator.calculateBill(units, PhaseType.SINGLE_PHASE, cycle, tariff)
        val threePhase = BillCalculator.calculateBill(units, PhaseType.THREE_PHASE, cycle, tariff)
        val difference = threePhase.totalAmount - singlePhase.totalAmount

        return PhaseCompareUiState(
            unitsInput = unitsInput,
            cycle = cycle,
            singlePhaseBreakdown = singlePhase,
            threePhaseBreakdown = threePhase,
            difference = difference,
            errorMessage = null,
            isCustomRates = isCustom
        )
    }
}
