package com.kseb.billcalculator.model

import java.util.UUID

data class Appliance(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val wattage: Double,
    val quantity: Int = 1,
    val hoursPerDay: Double = 1.0,
    val daysPerMonth: Int = 30
)

data class ApplianceItem(
    val appliance: Appliance,
    val monthlyKwh: Double
)

data class ApplianceResult(
    val items: List<ApplianceItem>,
    val totalUnits: Int
)

data class PresetAppliance(
    val name: String,
    val defaultWattage: Double
)

val PRESET_APPLIANCES = listOf(
    PresetAppliance("Ceiling Fan", 75.0),
    PresetAppliance("Table Fan", 50.0),
    PresetAppliance("AC (1 Ton)", 1000.0),
    PresetAppliance("AC (1.5 Ton)", 1500.0),
    PresetAppliance("Refrigerator", 200.0),
    PresetAppliance("Washing Machine", 500.0),
    PresetAppliance("LED TV (32\")", 50.0),
    PresetAppliance("LED TV (43\")", 80.0),
    PresetAppliance("LED Bulb (9W)", 9.0),
    PresetAppliance("LED Bulb (12W)", 12.0),
    PresetAppliance("Tube Light", 40.0),
    PresetAppliance("CFL", 25.0),
    PresetAppliance("Iron Box", 1000.0),
    PresetAppliance("Water Heater (Geyser)", 2000.0),
    PresetAppliance("Mixer Grinder", 750.0),
    PresetAppliance("Desktop Computer", 200.0),
    PresetAppliance("Laptop", 65.0),
    PresetAppliance("WiFi Router", 15.0),
    PresetAppliance("Mobile Charger", 10.0),
    PresetAppliance("Microwave Oven", 1200.0),
    PresetAppliance("Induction Cooker", 2000.0),
    PresetAppliance("Water Pump (0.5HP)", 375.0),
    PresetAppliance("Water Pump (1HP)", 750.0),
)
