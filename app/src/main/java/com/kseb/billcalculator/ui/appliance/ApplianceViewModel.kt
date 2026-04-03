package com.kseb.billcalculator.ui.appliance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kseb.billcalculator.calculation.ApplianceCalculator
import com.kseb.billcalculator.calculation.BillCalculator
import com.kseb.billcalculator.data.TariffPreferences
import com.kseb.billcalculator.model.Appliance
import com.kseb.billcalculator.model.ApplianceResult
import com.kseb.billcalculator.model.BillBreakdown
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.TariffConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ApplianceInputState(
    val name: String = "",
    val wattage: String = "",
    val quantity: String = "1",
    val hoursPerDay: String = "1",
    val daysPerMonth: String = "30"
)

data class ApplianceUiState(
    val appliances: List<Appliance> = emptyList(),
    val inputState: ApplianceInputState = ApplianceInputState(),
    val phase: PhaseType = PhaseType.SINGLE_PHASE,
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val applianceResult: ApplianceResult? = null,
    val billBreakdown: BillBreakdown? = null,
    val isCalculating: Boolean = false,
    val inputErrorMessage: String? = null,
    val errorMessage: String? = null,
    val isCustomRates: Boolean = false
)

class ApplianceViewModel(application: Application) : AndroidViewModel(application) {

    private val tariffPreferences = TariffPreferences(application)

    private val tariffConfig: StateFlow<TariffConfig> = tariffPreferences.tariffConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, TariffConfig.DEFAULT)

    private val _uiState = MutableStateFlow(ApplianceUiState())
    val uiState: StateFlow<ApplianceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tariffConfig.collect { config ->
                _uiState.value = _uiState.value.copy(isCustomRates = !config.isDefault())
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            inputState = _uiState.value.inputState.copy(name = name),
            inputErrorMessage = null
        )
    }

    fun updateWattage(wattage: String) {
        _uiState.value = _uiState.value.copy(
            inputState = _uiState.value.inputState.copy(wattage = wattage),
            inputErrorMessage = null
        )
    }

    fun updateQuantity(quantity: String) {
        _uiState.value = _uiState.value.copy(
            inputState = _uiState.value.inputState.copy(quantity = quantity),
            inputErrorMessage = null
        )
    }

    fun updateHoursPerDay(hours: String) {
        _uiState.value = _uiState.value.copy(
            inputState = _uiState.value.inputState.copy(hoursPerDay = hours),
            inputErrorMessage = null
        )
    }

    fun updateDaysPerMonth(days: String) {
        _uiState.value = _uiState.value.copy(
            inputState = _uiState.value.inputState.copy(daysPerMonth = days),
            inputErrorMessage = null
        )
    }

    fun updatePhase(phase: PhaseType) {
        _uiState.value = _uiState.value.copy(
            phase = phase,
            billBreakdown = null,
            applianceResult = null
        )
    }

    fun updateCycle(cycle: BillingCycle) {
        _uiState.value = _uiState.value.copy(
            cycle = cycle,
            billBreakdown = null,
            applianceResult = null
        )
    }

    fun addAppliance() {
        val input = _uiState.value.inputState

        // Validate name
        if (input.name.isBlank()) {
            _uiState.value = _uiState.value.copy(inputErrorMessage = "Please enter an appliance name")
            return
        }

        // Validate wattage
        val wattage = input.wattage.toDoubleOrNull()
        if (wattage == null || wattage <= 0) {
            _uiState.value = _uiState.value.copy(inputErrorMessage = "Please enter a valid wattage")
            return
        }

        // Validate quantity
        val quantity = input.quantity.toIntOrNull()
        if (quantity == null || quantity < 1 || quantity > 100) {
            _uiState.value = _uiState.value.copy(inputErrorMessage = "Quantity must be between 1 and 100")
            return
        }

        // Validate hours
        val hours = input.hoursPerDay.toDoubleOrNull()
        if (hours == null || hours <= 0 || hours > 24) {
            _uiState.value = _uiState.value.copy(inputErrorMessage = "Hours must be between 0 and 24")
            return
        }

        // Validate days
        val days = input.daysPerMonth.toIntOrNull()
        if (days == null || days < 1 || days > 31) {
            _uiState.value = _uiState.value.copy(inputErrorMessage = "Days must be between 1 and 31")
            return
        }

        val appliance = Appliance(
            name = input.name.trim(),
            wattage = wattage,
            quantity = quantity,
            hoursPerDay = hours,
            daysPerMonth = days
        )

        _uiState.value = _uiState.value.copy(
            appliances = _uiState.value.appliances + appliance,
            inputState = ApplianceInputState(),
            inputErrorMessage = null,
            billBreakdown = null,
            applianceResult = null
        )
    }

    fun addPresetAppliance(name: String, wattage: Double) {
        _uiState.value = _uiState.value.copy(
            inputState = ApplianceInputState(
                name = name,
                wattage = wattage.toString(),
                quantity = "1",
                hoursPerDay = "1",
                daysPerMonth = "30"
            ),
            inputErrorMessage = null
        )
    }

    fun removeAppliance(applianceId: String) {
        _uiState.value = _uiState.value.copy(
            appliances = _uiState.value.appliances.filter { it.id != applianceId },
            billBreakdown = null,
            applianceResult = null
        )
    }

    fun clearAllAppliances() {
        _uiState.value = _uiState.value.copy(
            appliances = emptyList(),
            applianceResult = null,
            billBreakdown = null
        )
    }

    fun calculate() {
        val state = _uiState.value

        if (state.appliances.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Please add at least one appliance")
            return
        }

        _uiState.value = state.copy(isCalculating = true, errorMessage = null)

        viewModelScope.launch {
            val applianceResult = ApplianceCalculator.calculateMonthlyUnits(state.appliances)
            val tariff = tariffConfig.value
            val breakdown = BillCalculator.calculateBill(
                units = applianceResult.totalUnits,
                phase = state.phase,
                cycle = state.cycle,
                tariff = tariff
            )

            _uiState.value = _uiState.value.copy(
                applianceResult = applianceResult,
                billBreakdown = breakdown,
                isCalculating = false,
                errorMessage = null
            )
        }
    }
}
