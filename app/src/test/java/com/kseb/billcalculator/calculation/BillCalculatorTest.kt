package com.kseb.billcalculator.calculation

import com.kseb.billcalculator.model.Appliance
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.ReverseResult
import com.kseb.billcalculator.model.TariffConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class BillCalculatorTest {

    private val tariff = TariffConfig.DEFAULT

    private fun bd(value: String): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)

    private fun assertBdEquals(expected: String, actual: BigDecimal, message: String = "") {
        val exp = bd(expected)
        assertTrue(
            "${if (message.isNotEmpty()) "$message: " else ""}expected <$exp> but was <$actual>",
            exp.compareTo(actual) == 0
        )
    }

    // ========================================================================
    // SINGLE PHASE, MONTHLY — TELESCOPIC RANGE (0-250 units)
    // ========================================================================

    @Test
    fun `0 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(0, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("0.00", bill.totalEnergyCharge)
        assertBdEquals("35.00", bill.fixedCharge)
        assertBdEquals("0.00", bill.electricityDuty)
        assertBdEquals("0.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("42.08", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `50 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(50, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("167.50", bill.totalEnergyCharge)
        assertBdEquals("35.00", bill.fixedCharge)
        assertBdEquals("16.75", bill.electricityDuty)
        assertBdEquals("5.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("231.33", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `100 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(100, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("380.00", bill.totalEnergyCharge)
        assertBdEquals("35.00", bill.fixedCharge)
        assertBdEquals("38.00", bill.electricityDuty)
        assertBdEquals("10.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("470.08", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `150 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(150, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("647.50", bill.totalEnergyCharge)
        assertBdEquals("55.00", bill.fixedCharge)
        assertBdEquals("64.75", bill.electricityDuty)
        assertBdEquals("15.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("789.33", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `200 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(200, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("1007.50", bill.totalEnergyCharge)
        assertBdEquals("55.00", bill.fixedCharge)
        assertBdEquals("100.75", bill.electricityDuty)
        assertBdEquals("20.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("1190.33", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `250 units single phase monthly - max telescopic`() {
        val bill = BillCalculator.calculateBill(250, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("1432.50", bill.totalEnergyCharge)
        assertBdEquals("75.00", bill.fixedCharge)
        assertBdEquals("143.25", bill.electricityDuty)
        assertBdEquals("25.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("1682.83", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    // ========================================================================
    // SINGLE PHASE, MONTHLY — NON-TELESCOPIC RANGE (251+ units)
    // ========================================================================

    @Test
    fun `251 units single phase monthly - non-telescopic boundary`() {
        val bill = BillCalculator.calculateBill(251, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("1694.25", bill.totalEnergyCharge)
        assertBdEquals("75.00", bill.fixedCharge)
        assertBdEquals("169.43", bill.electricityDuty) // 1694.25 * 0.10 = 169.425 -> 169.43
        assertBdEquals("25.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("1970.86", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `300 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(300, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("2025.00", bill.totalEnergyCharge)
        assertBdEquals("75.00", bill.fixedCharge)
        assertBdEquals("202.50", bill.electricityDuty)
        assertBdEquals("30.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("2339.58", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `301 units single phase monthly - band jump`() {
        val bill = BillCalculator.calculateBill(301, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("2287.60", bill.totalEnergyCharge)
        assertBdEquals("100.00", bill.fixedCharge)
        assertBdEquals("228.76", bill.electricityDuty)
        assertBdEquals("30.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("2653.54", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `350 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(350, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("2660.00", bill.totalEnergyCharge)
        assertBdEquals("100.00", bill.fixedCharge)
        assertBdEquals("266.00", bill.electricityDuty)
        assertBdEquals("35.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("3068.08", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `351 units single phase monthly - band jump`() {
        val bill = BillCalculator.calculateBill(351, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 351 * 7.95 = 2790.45
        assertBdEquals("2790.45", bill.totalEnergyCharge)
        assertBdEquals("100.00", bill.fixedCharge)
        // 2790.45 * 0.10 = 279.045 -> 279.05 (HALF_UP)
        assertBdEquals("279.05", bill.electricityDuty)
        assertBdEquals("35.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("3211.68", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `400 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(400, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("3180.00", bill.totalEnergyCharge)
        assertBdEquals("100.00", bill.fixedCharge)
        assertBdEquals("318.00", bill.electricityDuty)
        assertBdEquals("40.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("3645.08", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `401 units single phase monthly - band jump`() {
        val bill = BillCalculator.calculateBill(401, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 401 * 8.25 = 3308.25
        assertBdEquals("3308.25", bill.totalEnergyCharge)
        assertBdEquals("100.00", bill.fixedCharge)
        // 3308.25 * 0.10 = 330.825 -> 330.83 (HALF_UP)
        assertBdEquals("330.83", bill.electricityDuty)
        assertBdEquals("40.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("3786.26", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `500 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(500, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("4125.00", bill.totalEnergyCharge)
        assertBdEquals("100.00", bill.fixedCharge)
        assertBdEquals("412.50", bill.electricityDuty)
        assertBdEquals("50.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("4694.58", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `501 units single phase monthly - highest band`() {
        val bill = BillCalculator.calculateBill(501, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("4609.20", bill.totalEnergyCharge)
        assertBdEquals("125.00", bill.fixedCharge)
        assertBdEquals("460.92", bill.electricityDuty)
        assertBdEquals("50.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("5252.30", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    @Test
    fun `600 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(600, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 600 * 9.20 = 5520.00
        assertBdEquals("5520.00", bill.totalEnergyCharge)
        assertBdEquals("125.00", bill.fixedCharge)
        assertBdEquals("552.00", bill.electricityDuty)
        assertBdEquals("60.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("6264.08", bill.totalAmount)
        assertFalse(bill.isTelescopicBilling)
    }

    // ========================================================================
    // SINGLE PHASE, MONTHLY — SLAB BOUNDARY EDGE CASES
    // ========================================================================

    @Test
    fun `1 unit single phase monthly`() {
        val bill = BillCalculator.calculateBill(1, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 1 * 3.35 = 3.35
        assertBdEquals("3.35", bill.totalEnergyCharge)
        assertBdEquals("35.00", bill.fixedCharge)
        assertBdEquals("0.34", bill.electricityDuty) // 3.35 * 0.10 = 0.335 -> 0.34
        assertBdEquals("0.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("45.87", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `51 units single phase monthly - second slab starts`() {
        val bill = BillCalculator.calculateBill(51, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 50 * 3.35 + 1 * 4.25 = 167.50 + 4.25 = 171.75
        assertBdEquals("171.75", bill.totalEnergyCharge)
        assertBdEquals("35.00", bill.fixedCharge)
        assertBdEquals("17.18", bill.electricityDuty) // 171.75 * 0.10 = 17.175 -> 17.18
        assertBdEquals("5.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("236.11", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `101 units single phase monthly - fixed charge changes`() {
        val bill = BillCalculator.calculateBill(101, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 50 * 3.35 + 50 * 4.25 + 1 * 5.35 = 167.50 + 212.50 + 5.35 = 385.35
        assertBdEquals("385.35", bill.totalEnergyCharge)
        assertBdEquals("55.00", bill.fixedCharge) // fixed charge jumps from 35 to 55
        assertBdEquals("38.54", bill.electricityDuty) // 385.35 * 0.10 = 38.535 -> 38.54
        assertBdEquals("10.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("496.07", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    @Test
    fun `201 units single phase monthly - 5th slab starts, fixed charge changes`() {
        val bill = BillCalculator.calculateBill(201, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 167.50 + 212.50 + 267.50 + 360.00 + 1 * 8.50 = 1016.00
        assertBdEquals("1016.00", bill.totalEnergyCharge)
        assertBdEquals("75.00", bill.fixedCharge) // fixed charge jumps from 55 to 75
        assertBdEquals("101.60", bill.electricityDuty)
        assertBdEquals("20.10", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("1219.78", bill.totalAmount)
        assertTrue(bill.isTelescopicBilling)
    }

    // ========================================================================
    // THREE PHASE, MONTHLY TESTS
    // ========================================================================

    @Test
    fun `0 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(0, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("0.00", bill.totalEnergyCharge)
        assertBdEquals("85.00", bill.fixedCharge)
        assertBdEquals("0.00", bill.electricityDuty)
        assertBdEquals("0.00", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("102.70", bill.totalAmount)
    }

    @Test
    fun `100 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(100, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("380.00", bill.totalEnergyCharge)
        assertBdEquals("85.00", bill.fixedCharge)
        assertBdEquals("38.00", bill.electricityDuty)
        assertBdEquals("10.00", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("530.70", bill.totalAmount)
    }

    @Test
    fun `150 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(150, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("647.50", bill.totalEnergyCharge)
        assertBdEquals("130.00", bill.fixedCharge)
        assertBdEquals("64.75", bill.electricityDuty)
        assertBdEquals("15.00", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("874.95", bill.totalAmount)
    }

    @Test
    fun `250 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(250, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("1432.50", bill.totalEnergyCharge)
        assertBdEquals("160.00", bill.fixedCharge)
        assertBdEquals("143.25", bill.electricityDuty)
        assertBdEquals("25.00", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("1778.45", bill.totalAmount)
    }

    @Test
    fun `300 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(300, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("2025.00", bill.totalEnergyCharge)
        assertBdEquals("160.00", bill.fixedCharge)
        assertBdEquals("202.50", bill.electricityDuty)
        assertBdEquals("30.00", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("2435.20", bill.totalAmount)
    }

    @Test
    fun `501 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(501, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("4609.20", bill.totalEnergyCharge)
        assertBdEquals("250.00", bill.fixedCharge)
        assertBdEquals("460.92", bill.electricityDuty)
        assertBdEquals("50.10", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("5387.92", bill.totalAmount)
    }

    @Test
    fun `200 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(200, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("1007.50", bill.totalEnergyCharge)
        assertBdEquals("130.00", bill.fixedCharge)
        assertBdEquals("100.75", bill.electricityDuty)
        assertBdEquals("20.00", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("1275.95", bill.totalAmount)
    }

    @Test
    fun `301 units three phase monthly`() {
        val bill = BillCalculator.calculateBill(301, PhaseType.THREE_PHASE, BillingCycle.MONTHLY, tariff)
        assertBdEquals("2287.60", bill.totalEnergyCharge)
        assertBdEquals("200.00", bill.fixedCharge)
        assertBdEquals("228.76", bill.electricityDuty)
        assertBdEquals("30.10", bill.fuelSurcharge)
        assertBdEquals("15.00", bill.meterRent)
        assertBdEquals("2.70", bill.gstOnMeterRent)
        assertBdEquals("2764.16", bill.totalAmount)
    }

    // ========================================================================
    // BIMONTHLY BILLING TESTS
    // ========================================================================

    @Test
    fun `bimonthly 370 units - floor(370 div 2) = 185 monthly`() {
        // Monthly 185 units: energy = 167.50 + 212.50 + 267.50 + 35*7.20 = 899.50
        // fixed = 55, duty = 89.95, fuel = 18.50, meter = 6, gst = 1.08
        // monthly total = 1070.03
        // bimonthly = 2140.06
        val bill = BillCalculator.calculateBill(370, PhaseType.SINGLE_PHASE, BillingCycle.BIMONTHLY, tariff)
        assertBdEquals("1799.00", bill.totalEnergyCharge) // 899.50 * 2
        assertBdEquals("110.00", bill.fixedCharge) // 55 * 2
        assertBdEquals("179.90", bill.electricityDuty) // 89.95 * 2
        assertBdEquals("37.00", bill.fuelSurcharge) // 18.50 * 2
        assertBdEquals("12.00", bill.meterRent) // 6 * 2
        assertBdEquals("2.16", bill.gstOnMeterRent) // 1.08 * 2
        assertBdEquals("2140.06", bill.totalAmount)
    }

    @Test
    fun `bimonthly 501 units - floor(501 div 2) = 250 monthly (telescopic!)`() {
        // Monthly 250 units is telescopic: energy = 1432.50
        // fixed = 75, duty = 143.25, fuel = 25.00, meter = 6, gst = 1.08
        // monthly total = 1682.83
        // bimonthly = 3365.66
        val bill = BillCalculator.calculateBill(501, PhaseType.SINGLE_PHASE, BillingCycle.BIMONTHLY, tariff)
        assertTrue(bill.isTelescopicBilling) // 250 monthly is still telescopic
        assertBdEquals("2865.00", bill.totalEnergyCharge) // 1432.50 * 2
        assertBdEquals("150.00", bill.fixedCharge) // 75 * 2
        assertBdEquals("286.50", bill.electricityDuty) // 143.25 * 2
        assertBdEquals("50.00", bill.fuelSurcharge) // 25.00 * 2
        assertBdEquals("12.00", bill.meterRent)
        assertBdEquals("2.16", bill.gstOnMeterRent)
        assertBdEquals("3365.66", bill.totalAmount)
    }

    @Test
    fun `bimonthly 500 units - floor(500 div 2) = 250 monthly`() {
        val bill = BillCalculator.calculateBill(500, PhaseType.SINGLE_PHASE, BillingCycle.BIMONTHLY, tariff)
        assertTrue(bill.isTelescopicBilling)
        assertBdEquals("3365.66", bill.totalAmount) // same as 501 since floor(501/2) = floor(500/2) = 250
    }

    @Test
    fun `bimonthly 502 units - floor(502 div 2) = 251 monthly (non-telescopic!)`() {
        // Monthly 251 units: non-telescopic
        // energy = 251 * 6.75 = 1694.25, fixed = 75, duty = 169.43, fuel = 25.10
        // meter = 6, gst = 1.08, monthly total = 1970.86
        // bimonthly = 3941.72
        val bill = BillCalculator.calculateBill(502, PhaseType.SINGLE_PHASE, BillingCycle.BIMONTHLY, tariff)
        assertFalse(bill.isTelescopicBilling)
        assertBdEquals("3388.50", bill.totalEnergyCharge) // 1694.25 * 2
        assertBdEquals("150.00", bill.fixedCharge)
        assertBdEquals("338.86", bill.electricityDuty) // 169.43 * 2 (note: 169.425 * 2 = 338.85? Let's check)
        assertBdEquals("50.20", bill.fuelSurcharge)
        assertBdEquals("12.00", bill.meterRent)
        assertBdEquals("2.16", bill.gstOnMeterRent)
        assertBdEquals("3941.72", bill.totalAmount)
    }

    @Test
    fun `bimonthly 0 units single phase`() {
        val bill = BillCalculator.calculateBill(0, PhaseType.SINGLE_PHASE, BillingCycle.BIMONTHLY, tariff)
        assertBdEquals("0.00", bill.totalEnergyCharge)
        assertBdEquals("70.00", bill.fixedCharge) // 35 * 2
        assertBdEquals("0.00", bill.electricityDuty)
        assertBdEquals("0.00", bill.fuelSurcharge)
        assertBdEquals("12.00", bill.meterRent) // 6 * 2
        assertBdEquals("2.16", bill.gstOnMeterRent) // 1.08 * 2
        assertBdEquals("84.16", bill.totalAmount)
    }

    @Test
    fun `bimonthly odd units - floor division`() {
        // 371 bimonthly -> floor(371/2) = 185 monthly -> same as 370
        val bill371 = BillCalculator.calculateBill(371, PhaseType.SINGLE_PHASE, BillingCycle.BIMONTHLY, tariff)
        val bill370 = BillCalculator.calculateBill(370, PhaseType.SINGLE_PHASE, BillingCycle.BIMONTHLY, tariff)
        assertEquals(bill370.totalAmount, bill371.totalAmount)
    }

    @Test
    fun `bimonthly three phase 300 units`() {
        // floor(300/2) = 150 monthly, three phase
        // energy = 647.50, fixed = 130, duty = 64.75, fuel = 15.00, meter = 15, gst = 2.70
        // monthly = 874.95, bimonthly = 1749.90
        val bill = BillCalculator.calculateBill(300, PhaseType.THREE_PHASE, BillingCycle.BIMONTHLY, tariff)
        assertBdEquals("1295.00", bill.totalEnergyCharge) // 647.50 * 2
        assertBdEquals("260.00", bill.fixedCharge) // 130 * 2
        assertBdEquals("129.50", bill.electricityDuty)
        assertBdEquals("30.00", bill.fuelSurcharge)
        assertBdEquals("30.00", bill.meterRent)
        assertBdEquals("5.40", bill.gstOnMeterRent)
        assertBdEquals("1749.90", bill.totalAmount)
    }

    // ========================================================================
    // TELESCOPIC vs NON-TELESCOPIC BOUNDARY (250 -> 251)
    // ========================================================================

    @Test
    fun `250 to 251 boundary - bill jumps due to non-telescopic switch`() {
        val bill250 = BillCalculator.calculateBill(250, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        val bill251 = BillCalculator.calculateBill(251, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertTrue(bill250.isTelescopicBilling)
        assertFalse(bill251.isTelescopicBilling)
        // Energy jumps from 1432.50 (telescopic) to 1694.25 (non-telescopic)
        assertBdEquals("1432.50", bill250.totalEnergyCharge)
        assertBdEquals("1694.25", bill251.totalEnergyCharge)
        // Total jumps from 1682.83 to 1970.86 - a gap of ~288
        assertTrue(bill251.totalAmount > bill250.totalAmount)
        val gap = bill251.totalAmount - bill250.totalAmount
        assertTrue(gap > bd("280.00"))
    }

    // ========================================================================
    // SLAB DETAIL VERIFICATION
    // ========================================================================

    @Test
    fun `slab details for 175 units telescopic`() {
        val bill = BillCalculator.calculateBill(175, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertEquals(4, bill.slabDetails.size)
        assertEquals(50, bill.slabDetails[0].units)
        assertBdEquals("3.35", bill.slabDetails[0].rate)
        assertBdEquals("167.50", bill.slabDetails[0].charge)
        assertEquals(50, bill.slabDetails[1].units)
        assertBdEquals("4.25", bill.slabDetails[1].rate)
        assertBdEquals("212.50", bill.slabDetails[1].charge)
        assertEquals(50, bill.slabDetails[2].units)
        assertBdEquals("5.35", bill.slabDetails[2].rate)
        assertBdEquals("267.50", bill.slabDetails[2].charge)
        assertEquals(25, bill.slabDetails[3].units)
        assertBdEquals("7.20", bill.slabDetails[3].rate)
        assertBdEquals("180.00", bill.slabDetails[3].charge)
    }

    @Test
    fun `slab details for 300 units non-telescopic`() {
        val bill = BillCalculator.calculateBill(300, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        assertEquals(1, bill.slabDetails.size) // single flat rate
        assertEquals(300, bill.slabDetails[0].units)
        assertBdEquals("6.75", bill.slabDetails[0].rate)
        assertBdEquals("2025.00", bill.slabDetails[0].charge)
    }

    // ========================================================================
    // NEGATIVE / INVALID INPUT
    // ========================================================================

    @Test(expected = IllegalArgumentException::class)
    fun `negative units throws exception`() {
        BillCalculator.calculateBill(-1, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
    }

    // ========================================================================
    // VERY LARGE UNITS
    // ========================================================================

    @Test
    fun `1000 units single phase monthly`() {
        val bill = BillCalculator.calculateBill(1000, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff)
        // 1000 * 9.20 = 9200.00
        assertBdEquals("9200.00", bill.totalEnergyCharge)
        assertBdEquals("125.00", bill.fixedCharge)
        assertBdEquals("920.00", bill.electricityDuty)
        assertBdEquals("100.00", bill.fuelSurcharge)
        assertBdEquals("6.00", bill.meterRent)
        assertBdEquals("1.08", bill.gstOnMeterRent)
        assertBdEquals("10352.08", bill.totalAmount)
    }
}

class ApplianceCalculatorTest {

    @Test
    fun `fan 75W x2 x8h x30d = 36 kWh`() {
        val appliances = listOf(
            Appliance(
                name = "Ceiling Fan",
                wattage = 75.0,
                quantity = 2,
                hoursPerDay = 8.0,
                daysPerMonth = 30
            )
        )
        val result = ApplianceCalculator.calculateMonthlyUnits(appliances)
        assertEquals(1, result.items.size)
        assertEquals(36.0, result.items[0].monthlyKwh, 0.001)
        assertEquals(36, result.totalUnits)
    }

    @Test
    fun `single LED bulb 9W x1 x10h x30d rounds to 3 kWh`() {
        val appliances = listOf(
            Appliance(
                name = "LED Bulb",
                wattage = 9.0,
                quantity = 1,
                hoursPerDay = 10.0,
                daysPerMonth = 30
            )
        )
        val result = ApplianceCalculator.calculateMonthlyUnits(appliances)
        assertEquals(2.7, result.items[0].monthlyKwh, 0.001)
        assertEquals(3, result.totalUnits) // rounds to nearest int
    }

    @Test
    fun `multiple appliances total`() {
        val appliances = listOf(
            Appliance(name = "Fan", wattage = 75.0, quantity = 3, hoursPerDay = 8.0, daysPerMonth = 30),
            Appliance(name = "LED", wattage = 9.0, quantity = 5, hoursPerDay = 6.0, daysPerMonth = 30),
            Appliance(name = "Fridge", wattage = 200.0, quantity = 1, hoursPerDay = 24.0, daysPerMonth = 30)
        )
        val result = ApplianceCalculator.calculateMonthlyUnits(appliances)
        assertEquals(3, result.items.size)
        // Fan: 75 * 3 * 8 * 30 / 1000 = 54.0
        assertEquals(54.0, result.items[0].monthlyKwh, 0.001)
        // LED: 9 * 5 * 6 * 30 / 1000 = 8.1
        assertEquals(8.1, result.items[1].monthlyKwh, 0.001)
        // Fridge: 200 * 1 * 24 * 30 / 1000 = 144.0
        assertEquals(144.0, result.items[2].monthlyKwh, 0.001)
        // Total: 54 + 8.1 + 144 = 206.1 -> 206
        assertEquals(206, result.totalUnits)
    }

    @Test
    fun `empty appliance list`() {
        val result = ApplianceCalculator.calculateMonthlyUnits(emptyList())
        assertEquals(0, result.items.size)
        assertEquals(0, result.totalUnits)
    }

    @Test
    fun `AC 1500W x1 x8h x30d = 360 kWh`() {
        val appliances = listOf(
            Appliance(name = "AC", wattage = 1500.0, quantity = 1, hoursPerDay = 8.0, daysPerMonth = 30)
        )
        val result = ApplianceCalculator.calculateMonthlyUnits(appliances)
        assertEquals(360.0, result.items[0].monthlyKwh, 0.001)
        assertEquals(360, result.totalUnits)
    }
}

class ReverseCalculatorTest {

    private val tariff = TariffConfig.DEFAULT

    private fun bd(value: String): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)

    private fun assertBdEquals(expected: String, actual: BigDecimal, message: String = "") {
        val exp = bd(expected)
        assertTrue(
            "${if (message.isNotEmpty()) "$message: " else ""}expected <$exp> but was <$actual>",
            exp.compareTo(actual) == 0
        )
    }

    // ========================================================================
    // ROUND-TRIP INVARIANT: reverse(forward(units)) == units
    // ========================================================================

    @Test
    fun `round trip 0 units single phase monthly`() {
        assertRoundTrip(0, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 50 units single phase monthly`() {
        assertRoundTrip(50, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 100 units single phase monthly`() {
        assertRoundTrip(100, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 150 units single phase monthly`() {
        assertRoundTrip(150, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 200 units single phase monthly`() {
        assertRoundTrip(200, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 250 units single phase monthly`() {
        assertRoundTrip(250, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 300 units single phase monthly`() {
        assertRoundTrip(300, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 400 units single phase monthly`() {
        assertRoundTrip(400, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 500 units single phase monthly`() {
        assertRoundTrip(500, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 501 units single phase monthly`() {
        assertRoundTrip(501, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 600 units single phase monthly`() {
        assertRoundTrip(600, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 251 units single phase monthly`() {
        assertRoundTrip(251, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 301 units single phase monthly`() {
        assertRoundTrip(301, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 351 units single phase monthly`() {
        assertRoundTrip(351, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 401 units single phase monthly`() {
        assertRoundTrip(401, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY)
    }

    // Three phase round trips
    @Test
    fun `round trip 100 units three phase monthly`() {
        assertRoundTrip(100, PhaseType.THREE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 250 units three phase monthly`() {
        assertRoundTrip(250, PhaseType.THREE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 300 units three phase monthly`() {
        assertRoundTrip(300, PhaseType.THREE_PHASE, BillingCycle.MONTHLY)
    }

    @Test
    fun `round trip 501 units three phase monthly`() {
        assertRoundTrip(501, PhaseType.THREE_PHASE, BillingCycle.MONTHLY)
    }

    // ========================================================================
    // BELOW MINIMUM BILL
    // ========================================================================

    @Test
    fun `reverse zero amount returns below minimum`() {
        val result = ReverseCalculator.reverseBill(
            bd("0.00"), PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff
        )
        assertTrue(result is ReverseResult.BelowMinimum)
    }

    @Test
    fun `reverse negative amount returns below minimum`() {
        val result = ReverseCalculator.reverseBill(
            bd("-5.00"), PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff
        )
        assertTrue(result is ReverseResult.BelowMinimum)
    }

    @Test
    fun `reverse amount below minimum bill returns below minimum`() {
        val result = ReverseCalculator.reverseBill(
            bd("10.00"), PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff
        )
        assertTrue(result is ReverseResult.BelowMinimum)
        val belowMin = result as ReverseResult.BelowMinimum
        assertBdEquals("42.08", belowMin.minimumBill)
    }

    // ========================================================================
    // GAP DETECTION (250-251 boundary)
    // ========================================================================

    @Test
    fun `reverse amount in 250-251 gap returns gap result`() {
        // Amount between 250-unit bill (1682.83) and 251-unit bill (1970.86)
        val gapAmount = bd("1800.00")
        val result = ReverseCalculator.reverseBill(
            gapAmount, PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff
        )
        assertTrue("Expected Gap result for amount in 250-251 boundary", result is ReverseResult.Gap)
        val gap = result as ReverseResult.Gap
        // Should have lower option at 250 and upper option at 251
        assertTrue(gap.lowerOption != null)
        assertTrue(gap.upperOption != null)
        assertEquals(250, gap.lowerOption!!.units)
        assertEquals(251, gap.upperOption!!.units)
    }

    // ========================================================================
    // EXACT MATCH AT KNOWN AMOUNTS
    // ========================================================================

    @Test
    fun `reverse exact amount for 0 units`() {
        val result = ReverseCalculator.reverseBill(
            bd("42.08"), PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff
        )
        assertTrue(result is ReverseResult.Match)
        assertEquals(0, (result as ReverseResult.Match).units)
    }

    @Test
    fun `reverse exact amount for 100 units`() {
        val result = ReverseCalculator.reverseBill(
            bd("470.08"), PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff
        )
        assertTrue(result is ReverseResult.Match)
        assertEquals(100, (result as ReverseResult.Match).units)
    }

    @Test
    fun `reverse exact amount for 500 units`() {
        val result = ReverseCalculator.reverseBill(
            bd("4694.58"), PhaseType.SINGLE_PHASE, BillingCycle.MONTHLY, tariff
        )
        assertTrue(result is ReverseResult.Match)
        assertEquals(500, (result as ReverseResult.Match).units)
    }

    private fun assertRoundTrip(units: Int, phase: PhaseType, cycle: BillingCycle) {
        val bill = BillCalculator.calculateBill(units, phase, cycle, tariff)
        val result = ReverseCalculator.reverseBill(bill.totalAmount, phase, cycle, tariff)
        assertTrue(
            "Round trip failed for $units units ($phase, $cycle): result was $result",
            result is ReverseResult.Match
        )
        assertEquals(
            "Round trip units mismatch for $units units ($phase, $cycle)",
            units,
            (result as ReverseResult.Match).units
        )
    }
}
