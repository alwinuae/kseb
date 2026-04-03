package com.kseb.billcalculator.ui.unitstobill

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

data class UnitsToBillUiState(
    val unitsInput: String = "",
    val phase: PhaseType = PhaseType.SINGLE_PHASE,
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val breakdown: BillBreakdown? = null,
    val errorMessage: String? = null,
    val isCustomRates: Boolean = false
)

@OptIn(FlowPreview::class)
class UnitsToBillViewModel(application: Application) : AndroidViewModel(application) {

    private val tariffPreferences = TariffPreferences(application)

    private val _unitsInput = MutableStateFlow("")
    private val _phase = MutableStateFlow(PhaseType.SINGLE_PHASE)
    private val _cycle = MutableStateFlow(BillingCycle.MONTHLY)

    private val tariffConfig: StateFlow<TariffConfig> = tariffPreferences.tariffConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, TariffConfig.DEFAULT)

    private val debouncedUnits = _unitsInput.debounce(300)

    val uiState: StateFlow<UnitsToBillUiState> = combine(
        debouncedUnits,
        _phase,
        _cycle,
        tariffConfig
    ) { units, phase, cycle, tariff ->
        calculateState(units, phase, cycle, tariff, !tariff.isDefault())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UnitsToBillUiState()
    )

    private val _immediateState = MutableStateFlow(UnitsToBillUiState())
    val immediateInputState: StateFlow<UnitsToBillUiState> = _immediateState.asStateFlow()

    fun updateUnits(input: String) {
        _unitsInput.value = input
        _immediateState.value = _immediateState.value.copy(unitsInput = input)
    }

    fun updatePhase(phase: PhaseType) {
        _phase.value = phase
        _immediateState.value = _immediateState.value.copy(phase = phase)
    }

    fun updateCycle(cycle: BillingCycle) {
        _cycle.value = cycle
        _immediateState.value = _immediateState.value.copy(cycle = cycle)
    }

    private fun calculateState(
        unitsInput: String,
        phase: PhaseType,
        cycle: BillingCycle,
        tariff: TariffConfig,
        isCustomRates: Boolean = false
    ): UnitsToBillUiState {
        if (unitsInput.isBlank()) {
            return UnitsToBillUiState(
                unitsInput = unitsInput,
                phase = phase,
                cycle = cycle,
                breakdown = null,
                errorMessage = null,
                isCustomRates = isCustomRates
            )
        }

        val units = unitsInput.toIntOrNull()
        if (units == null || units < 0 || units > 10000) {
            return UnitsToBillUiState(
                unitsInput = unitsInput,
                phase = phase,
                cycle = cycle,
                breakdown = null,
                errorMessage = when {
                    units == null -> "Please enter a valid number"
                    units < 0 -> "Units cannot be negative"
                    else -> "Units must be 10,000 or less"
                },
                isCustomRates = isCustomRates
            )
        }

        val breakdown = BillCalculator.calculateBill(units, phase, cycle, tariff)
        return UnitsToBillUiState(
            unitsInput = unitsInput,
            phase = phase,
            cycle = cycle,
            breakdown = breakdown,
            errorMessage = null,
            isCustomRates = isCustomRates
        )
    }
}
