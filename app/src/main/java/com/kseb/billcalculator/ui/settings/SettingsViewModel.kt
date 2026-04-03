package com.kseb.billcalculator.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kseb.billcalculator.data.TariffPreferences
import com.kseb.billcalculator.model.FixedChargeRange
import com.kseb.billcalculator.model.SlabRate
import com.kseb.billcalculator.model.TariffConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val telescopicRates: List<String> = listOf("3.35", "4.25", "5.35", "7.20", "8.50"),
    val nonTelescopicRates: List<String> = listOf("6.75", "7.60", "7.95", "8.25", "9.20"),
    val fixedChargesSingle: List<String> = listOf("35.0", "55.0", "75.0", "100.0", "125.0"),
    val fixedChargesThree: List<String> = listOf("85.0", "130.0", "160.0", "200.0", "250.0"),
    val electricityDutyPercent: String = "10.0",
    val fuelSurchargePerUnit: String = "0.10",
    val meterRentSingle: String = "6.0",
    val meterRentThree: String = "15.0",
    val gstPercent: String = "18.0",
    val isCustomRates: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val tariffPreferences = TariffPreferences(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val tariffConfig: StateFlow<TariffConfig> = tariffPreferences.tariffConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, TariffConfig.DEFAULT)

    init {
        viewModelScope.launch {
            tariffPreferences.tariffConfig.collect { config ->
                _uiState.update { configToUiState(config) }
            }
        }
    }

    private fun configToUiState(config: TariffConfig): SettingsUiState {
        return SettingsUiState(
            telescopicRates = config.telescopicSlabs.map { it.rate.toString() },
            nonTelescopicRates = config.nonTelescopicSlabs.map { it.rate.toString() },
            fixedChargesSingle = config.fixedChargeRanges.map { it.singlePhaseCharge.toString() },
            fixedChargesThree = config.fixedChargeRanges.map { it.threePhaseCharge.toString() },
            electricityDutyPercent = config.electricityDutyPercent.toString(),
            fuelSurchargePerUnit = config.fuelSurchargePerUnit.toString(),
            meterRentSingle = config.meterRentSinglePhase.toString(),
            meterRentThree = config.meterRentThreePhase.toString(),
            gstPercent = config.gstOnMeterRentPercent.toString(),
            isCustomRates = !config.isDefault(),
            isSaved = false,
            errorMessage = null
        )
    }

    fun updateTelescopicRate(index: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                telescopicRates = state.telescopicRates.toMutableList().also { it[index] = value },
                isSaved = false
            )
        }
    }

    fun updateNonTelescopicRate(index: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                nonTelescopicRates = state.nonTelescopicRates.toMutableList().also { it[index] = value },
                isSaved = false
            )
        }
    }

    fun updateFixedChargeSingle(index: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                fixedChargesSingle = state.fixedChargesSingle.toMutableList().also { it[index] = value },
                isSaved = false
            )
        }
    }

    fun updateFixedChargeThree(index: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                fixedChargesThree = state.fixedChargesThree.toMutableList().also { it[index] = value },
                isSaved = false
            )
        }
    }

    fun updateElectricityDuty(value: String) {
        _uiState.update { it.copy(electricityDutyPercent = value, isSaved = false) }
    }

    fun updateFuelSurcharge(value: String) {
        _uiState.update { it.copy(fuelSurchargePerUnit = value, isSaved = false) }
    }

    fun updateMeterRentSingle(value: String) {
        _uiState.update { it.copy(meterRentSingle = value, isSaved = false) }
    }

    fun updateMeterRentThree(value: String) {
        _uiState.update { it.copy(meterRentThree = value, isSaved = false) }
    }

    fun updateGstPercent(value: String) {
        _uiState.update { it.copy(gstPercent = value, isSaved = false) }
    }

    fun save() {
        val state = _uiState.value

        // Validate all fields are valid numbers
        val telescopic = state.telescopicRates.map { it.toDoubleOrNull() }
        val nonTelescopic = state.nonTelescopicRates.map { it.toDoubleOrNull() }
        val fixedSingle = state.fixedChargesSingle.map { it.toDoubleOrNull() }
        val fixedThree = state.fixedChargesThree.map { it.toDoubleOrNull() }
        val dutyPercent = state.electricityDutyPercent.toDoubleOrNull()
        val fuelSurcharge = state.fuelSurchargePerUnit.toDoubleOrNull()
        val meterSingle = state.meterRentSingle.toDoubleOrNull()
        val meterThree = state.meterRentThree.toDoubleOrNull()
        val gst = state.gstPercent.toDoubleOrNull()

        if (telescopic.any { it == null } || nonTelescopic.any { it == null } ||
            fixedSingle.any { it == null } || fixedThree.any { it == null } ||
            dutyPercent == null || fuelSurcharge == null ||
            meterSingle == null || meterThree == null || gst == null
        ) {
            _uiState.update { it.copy(errorMessage = "All fields must contain valid numbers") }
            return
        }

        if (telescopic.any { it!! < 0 } || nonTelescopic.any { it!! < 0 } ||
            fixedSingle.any { it!! < 0 } || fixedThree.any { it!! < 0 } ||
            dutyPercent < 0 || fuelSurcharge < 0 ||
            meterSingle < 0 || meterThree < 0 || gst < 0
        ) {
            _uiState.update { it.copy(errorMessage = "Values cannot be negative") }
            return
        }

        val defaultTelescopic = TariffConfig.DEFAULT_TELESCOPIC_SLABS
        val defaultNonTelescopic = TariffConfig.DEFAULT_NON_TELESCOPIC_SLABS
        val defaultFixed = TariffConfig.DEFAULT_FIXED_CHARGES

        val config = TariffConfig(
            telescopicSlabs = defaultTelescopic.mapIndexed { i, slab ->
                SlabRate(slab.lowerLimit, slab.upperLimit, telescopic[i]!!)
            },
            nonTelescopicSlabs = defaultNonTelescopic.mapIndexed { i, slab ->
                SlabRate(slab.lowerLimit, slab.upperLimit, nonTelescopic[i]!!)
            },
            fixedChargeRanges = defaultFixed.mapIndexed { i, range ->
                FixedChargeRange(range.lowerLimit, range.upperLimit, fixedSingle[i]!!, fixedThree[i]!!)
            },
            electricityDutyPercent = dutyPercent,
            fuelSurchargePerUnit = fuelSurcharge,
            meterRentSinglePhase = meterSingle,
            meterRentThreePhase = meterThree,
            gstOnMeterRentPercent = gst
        )

        viewModelScope.launch {
            tariffPreferences.saveTariffConfig(config)
            _uiState.update { it.copy(isSaved = true, errorMessage = null, isCustomRates = !config.isDefault()) }
            delay(2000)
            _uiState.update { it.copy(isSaved = false) }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            tariffPreferences.resetToDefaults()
            _uiState.update { configToUiState(TariffConfig.DEFAULT) }
        }
    }
}
