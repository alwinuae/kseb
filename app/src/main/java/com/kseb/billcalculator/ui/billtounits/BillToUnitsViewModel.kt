package com.kseb.billcalculator.ui.billtounits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kseb.billcalculator.calculation.ReverseCalculator
import com.kseb.billcalculator.data.TariffPreferences
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.ReverseResult
import com.kseb.billcalculator.model.TariffConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class BillToUnitsUiState(
    val billAmountInput: String = "",
    val phase: PhaseType = PhaseType.SINGLE_PHASE,
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val result: ReverseResult? = null,
    val isCalculating: Boolean = false,
    val errorMessage: String? = null,
    val isCustomRates: Boolean = false
)

class BillToUnitsViewModel(application: Application) : AndroidViewModel(application) {

    private val tariffPreferences = TariffPreferences(application)

    private val tariffConfig: StateFlow<TariffConfig> = tariffPreferences.tariffConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, TariffConfig.DEFAULT)

    private val _uiState = MutableStateFlow(BillToUnitsUiState())
    val uiState: StateFlow<BillToUnitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tariffConfig.collect { tariff ->
                _uiState.value = _uiState.value.copy(
                    isCustomRates = !tariff.isDefault()
                )
            }
        }
    }

    fun updateBillAmount(input: String) {
        _uiState.value = _uiState.value.copy(
            billAmountInput = input,
            result = null,
            errorMessage = null
        )
    }

    fun updatePhase(phase: PhaseType) {
        _uiState.value = _uiState.value.copy(
            phase = phase,
            result = null
        )
    }

    fun updateCycle(cycle: BillingCycle) {
        _uiState.value = _uiState.value.copy(
            cycle = cycle,
            result = null
        )
    }

    fun calculate() {
        val state = _uiState.value
        val amountStr = state.billAmountInput.trim()

        if (amountStr.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a bill amount")
            return
        }

        val amount = try {
            BigDecimal(amountStr)
        } catch (_: NumberFormatException) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid number")
            return
        }

        if (amount < BigDecimal.ZERO) {
            _uiState.value = state.copy(errorMessage = "Amount cannot be negative")
            return
        }

        if (amount > BigDecimal("1000000")) {
            _uiState.value = state.copy(errorMessage = "Amount seems too large")
            return
        }

        _uiState.value = state.copy(isCalculating = true, errorMessage = null)

        viewModelScope.launch {
            val tariff = tariffConfig.value
            val result = ReverseCalculator.reverseBill(
                totalAmount = amount,
                phase = state.phase,
                cycle = state.cycle,
                tariff = tariff
            )
            _uiState.value = _uiState.value.copy(
                result = result,
                isCalculating = false
            )
        }
    }
}
