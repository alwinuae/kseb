package com.kseb.billcalculator.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.kseb.billcalculator.model.ApplianceResult
import com.kseb.billcalculator.model.BillBreakdown
import com.kseb.billcalculator.model.ReverseResult
import com.kseb.billcalculator.ui.components.formatIndianCurrency
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private fun titlePaint(): Paint = Paint().apply {
        color = Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private fun headingPaint(): Paint = Paint().apply {
        color = Color.BLACK
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private fun normalPaint(): Paint = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    private fun smallPaint(): Paint = Paint().apply {
        color = Color.DKGRAY
        textSize = 9f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    private fun boldPaint(): Paint = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private fun totalPaint(): Paint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private fun dateString(): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())

    private fun timestampFilename(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    private fun ensurePdfDir(context: Context): File {
        val dir = File(context.cacheDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun drawHeader(pageManager: PdfPageManager, subtitle: String, isCustomRates: Boolean) {
        PdfTextBlock.drawText(pageManager, "KSEB BILL ESTIMATE", titlePaint(), TextAlignment.CENTER)
        PdfTextBlock.drawSpacer(pageManager, 4f)
        PdfTextBlock.drawText(pageManager, subtitle, headingPaint(), TextAlignment.CENTER)
        PdfTextBlock.drawSpacer(pageManager, 4f)
        PdfTextBlock.drawText(pageManager, "Generated: ${dateString()}", smallPaint(), TextAlignment.CENTER)
        if (isCustomRates) {
            PdfTextBlock.drawSpacer(pageManager, 2f)
            val customPaint = Paint(smallPaint()).apply { color = Color.rgb(180, 100, 0) }
            PdfTextBlock.drawText(
                pageManager,
                "Calculated using user-customized tariff rates",
                customPaint,
                TextAlignment.CENTER
            )
        }
        PdfTextBlock.drawSpacer(pageManager, 8f)
        PdfTextBlock.drawDivider(pageManager)
        PdfTextBlock.drawSpacer(pageManager, 8f)
    }

    private fun drawDisclaimer(pageManager: PdfPageManager) {
        PdfTextBlock.drawSpacer(pageManager, 16f)
        PdfTextBlock.drawDivider(pageManager)
        PdfTextBlock.drawSpacer(pageManager, 6f)
        val disclaimerPaint = Paint(smallPaint()).apply {
            color = Color.rgb(180, 0, 0)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        PdfTextBlock.drawText(
            pageManager,
            "ESTIMATE ONLY - This is not an official KSEB bill.",
            disclaimerPaint,
            TextAlignment.CENTER
        )
        PdfTextBlock.drawText(
            pageManager,
            "Actual bill may vary. For official calculations, contact KSEB.",
            smallPaint(),
            TextAlignment.CENTER
        )
    }

    private fun drawBreakdownDetails(
        pageManager: PdfPageManager,
        breakdown: BillBreakdown
    ) {
        // Connection info
        PdfTextBlock.drawText(pageManager, "Connection Details", headingPaint())
        PdfTextBlock.drawSpacer(pageManager, 4f)
        PdfTextBlock.drawKeyValue(pageManager, "Connection Type:", breakdown.phase.displayName, normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(pageManager, "Billing Cycle:", breakdown.cycle.displayName, normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(pageManager, "Units Consumed:", "${breakdown.units}", normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(
            pageManager,
            "Billing Method:",
            if (breakdown.isTelescopicBilling) "Telescopic (Slab-wise)" else "Non-Telescopic (Flat rate)",
            normalPaint(),
            normalPaint()
        )
        PdfTextBlock.drawSpacer(pageManager, 12f)

        // Slab table
        if (breakdown.slabDetails.isNotEmpty()) {
            PdfTextBlock.drawText(pageManager, "Energy Charges Breakdown", headingPaint())
            PdfTextBlock.drawSpacer(pageManager, 6f)

            val headers = listOf("Slab", "Rate (per unit)", "Units", "Charge")
            val rows = breakdown.slabDetails.map { slab ->
                listOf(
                    slab.label,
                    formatIndianCurrency(slab.rate),
                    "${slab.units}",
                    formatIndianCurrency(slab.charge)
                )
            }
            val colWidths = listOf(160f, 120f, 80f, 155f)
            PdfTableBuilder.drawTable(pageManager, headers, rows, colWidths, normalPaint())
            PdfTextBlock.drawSpacer(pageManager, 12f)
        }

        // Bill summary
        PdfTextBlock.drawText(pageManager, "Bill Summary", headingPaint())
        PdfTextBlock.drawSpacer(pageManager, 6f)
        PdfTextBlock.drawDivider(pageManager)

        PdfTextBlock.drawKeyValue(pageManager, "Energy Charge:", formatIndianCurrency(breakdown.totalEnergyCharge), normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(pageManager, "Fixed Charge:", formatIndianCurrency(breakdown.fixedCharge), normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(pageManager, "Electricity Duty:", formatIndianCurrency(breakdown.electricityDuty), normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(pageManager, "Fuel Surcharge:", formatIndianCurrency(breakdown.fuelSurcharge), normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(pageManager, "Meter Rent:", formatIndianCurrency(breakdown.meterRent), normalPaint(), normalPaint())
        PdfTextBlock.drawKeyValue(pageManager, "GST on Meter Rent:", formatIndianCurrency(breakdown.gstOnMeterRent), normalPaint(), normalPaint())

        PdfTextBlock.drawDivider(pageManager)
        PdfTextBlock.drawKeyValue(
            pageManager,
            "TOTAL AMOUNT:",
            formatIndianCurrency(breakdown.totalAmount),
            totalPaint(),
            totalPaint()
        )
        PdfTextBlock.drawDivider(pageManager)
    }

    fun generateBillPdf(
        context: Context,
        breakdown: BillBreakdown,
        isCustomRates: Boolean
    ): File {
        val pdfDocument = PdfDocument()
        val pageManager = PdfPageManager(pdfDocument)
        pageManager.startNewPage()

        drawHeader(pageManager, "Units to Bill Calculation", isCustomRates)
        drawBreakdownDetails(pageManager, breakdown)
        drawDisclaimer(pageManager)

        pageManager.finishPage()

        val file = File(ensurePdfDir(context), "bill_${timestampFilename()}.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }

    fun generateAppliancePdf(
        context: Context,
        applianceResult: ApplianceResult,
        breakdown: BillBreakdown,
        isCustomRates: Boolean
    ): File {
        val pdfDocument = PdfDocument()
        val pageManager = PdfPageManager(pdfDocument)
        pageManager.startNewPage()

        drawHeader(pageManager, "Appliance-Based Bill Estimate", isCustomRates)

        // Appliance list table
        PdfTextBlock.drawText(pageManager, "Appliance Usage", headingPaint())
        PdfTextBlock.drawSpacer(pageManager, 6f)

        val headers = listOf("Appliance", "Wattage", "Hrs/Day", "Qty", "kWh/Month")
        val rows = applianceResult.items.map { item ->
            listOf(
                item.appliance.name,
                "${item.appliance.wattage.toInt()}W",
                String.format(Locale.US, "%.1f", item.appliance.hoursPerDay),
                "${item.appliance.quantity}",
                String.format(Locale.US, "%.1f", item.monthlyKwh)
            )
        }
        val colWidths = listOf(155f, 75f, 75f, 55f, 155f)
        PdfTableBuilder.drawTable(pageManager, headers, rows, colWidths, normalPaint())

        PdfTextBlock.drawSpacer(pageManager, 8f)
        PdfTextBlock.drawKeyValue(
            pageManager,
            "Total Monthly Consumption:",
            "${applianceResult.totalUnits} units",
            boldPaint(),
            boldPaint()
        )
        PdfTextBlock.drawSpacer(pageManager, 16f)

        // Bill breakdown
        drawBreakdownDetails(pageManager, breakdown)
        drawDisclaimer(pageManager)

        pageManager.finishPage()

        val file = File(ensurePdfDir(context), "appliance_bill_${timestampFilename()}.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }

    fun generateReversePdf(
        context: Context,
        result: ReverseResult.Match,
        isCustomRates: Boolean
    ): File {
        val pdfDocument = PdfDocument()
        val pageManager = PdfPageManager(pdfDocument)
        pageManager.startNewPage()

        drawHeader(pageManager, "Reverse Calculation (Bill to Units)", isCustomRates)

        // Input info
        PdfTextBlock.drawText(pageManager, "Reverse Calculation Result", headingPaint())
        PdfTextBlock.drawSpacer(pageManager, 6f)
        PdfTextBlock.drawKeyValue(
            pageManager,
            "Estimated Units:",
            "${result.units}",
            normalPaint(),
            boldPaint()
        )
        if (result.difference.compareTo(BigDecimal.ZERO) != 0) {
            PdfTextBlock.drawKeyValue(
                pageManager,
                "Rounding Difference:",
                formatIndianCurrency(result.difference),
                normalPaint(),
                normalPaint()
            )
        }
        result.note?.let { note ->
            PdfTextBlock.drawSpacer(pageManager, 4f)
            PdfTextBlock.drawText(pageManager, "Note: $note", smallPaint())
        }
        PdfTextBlock.drawSpacer(pageManager, 12f)

        // Verification breakdown
        PdfTextBlock.drawText(pageManager, "Verification (Forward Calculation)", headingPaint())
        PdfTextBlock.drawSpacer(pageManager, 6f)
        drawBreakdownDetails(pageManager, result.breakdown)
        drawDisclaimer(pageManager)

        pageManager.finishPage()

        val file = File(ensurePdfDir(context), "reverse_bill_${timestampFilename()}.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }

    fun generateComparisonPdf(
        context: Context,
        singlePhase: BillBreakdown,
        threePhase: BillBreakdown,
        isCustomRates: Boolean
    ): File {
        val pdfDocument = PdfDocument()
        val pageManager = PdfPageManager(pdfDocument)
        pageManager.startNewPage()

        drawHeader(pageManager, "Phase Comparison", isCustomRates)

        // Summary comparison table
        PdfTextBlock.drawText(pageManager, "Side-by-Side Comparison", headingPaint())
        PdfTextBlock.drawSpacer(pageManager, 6f)

        val headers = listOf("Component", "Single Phase", "Three Phase")
        val rows = listOf(
            listOf("Units", "${singlePhase.units}", "${threePhase.units}"),
            listOf("Billing Cycle", singlePhase.cycle.displayName, threePhase.cycle.displayName),
            listOf(
                "Billing Method",
                if (singlePhase.isTelescopicBilling) "Telescopic" else "Non-Telescopic",
                if (threePhase.isTelescopicBilling) "Telescopic" else "Non-Telescopic"
            ),
            listOf("Energy Charge", formatIndianCurrency(singlePhase.totalEnergyCharge), formatIndianCurrency(threePhase.totalEnergyCharge)),
            listOf("Fixed Charge", formatIndianCurrency(singlePhase.fixedCharge), formatIndianCurrency(threePhase.fixedCharge)),
            listOf("Electricity Duty", formatIndianCurrency(singlePhase.electricityDuty), formatIndianCurrency(threePhase.electricityDuty)),
            listOf("Fuel Surcharge", formatIndianCurrency(singlePhase.fuelSurcharge), formatIndianCurrency(threePhase.fuelSurcharge)),
            listOf("Meter Rent", formatIndianCurrency(singlePhase.meterRent), formatIndianCurrency(threePhase.meterRent)),
            listOf("GST on Meter Rent", formatIndianCurrency(singlePhase.gstOnMeterRent), formatIndianCurrency(threePhase.gstOnMeterRent)),
            listOf("TOTAL", formatIndianCurrency(singlePhase.totalAmount), formatIndianCurrency(threePhase.totalAmount))
        )
        val colWidths = listOf(165f, 175f, 175f)
        PdfTableBuilder.drawTable(pageManager, headers, rows, colWidths, normalPaint())

        PdfTextBlock.drawSpacer(pageManager, 12f)

        // Difference
        val diff = threePhase.totalAmount.subtract(singlePhase.totalAmount)
        val diffLabel = if (diff.signum() > 0) {
            "Three Phase costs ${formatIndianCurrency(diff)} more"
        } else if (diff.signum() < 0) {
            "Single Phase costs ${formatIndianCurrency(diff.abs())} more"
        } else {
            "Both phases cost the same"
        }
        PdfTextBlock.drawKeyValue(pageManager, "Difference:", diffLabel, boldPaint(), boldPaint())

        PdfTextBlock.drawSpacer(pageManager, 16f)

        // Single Phase slab details
        if (singlePhase.slabDetails.isNotEmpty()) {
            PdfTextBlock.drawText(pageManager, "Single Phase - Slab Breakdown", headingPaint())
            PdfTextBlock.drawSpacer(pageManager, 6f)
            val slabHeaders = listOf("Slab", "Rate", "Units", "Charge")
            val slabRows = singlePhase.slabDetails.map { slab ->
                listOf(slab.label, formatIndianCurrency(slab.rate), "${slab.units}", formatIndianCurrency(slab.charge))
            }
            PdfTableBuilder.drawTable(pageManager, slabHeaders, slabRows, listOf(160f, 120f, 80f, 155f), normalPaint())
            PdfTextBlock.drawSpacer(pageManager, 12f)
        }

        // Three Phase slab details
        if (threePhase.slabDetails.isNotEmpty()) {
            PdfTextBlock.drawText(pageManager, "Three Phase - Slab Breakdown", headingPaint())
            PdfTextBlock.drawSpacer(pageManager, 6f)
            val slabHeaders = listOf("Slab", "Rate", "Units", "Charge")
            val slabRows = threePhase.slabDetails.map { slab ->
                listOf(slab.label, formatIndianCurrency(slab.rate), "${slab.units}", formatIndianCurrency(slab.charge))
            }
            PdfTableBuilder.drawTable(pageManager, slabHeaders, slabRows, listOf(160f, 120f, 80f, 155f), normalPaint())
        }

        drawDisclaimer(pageManager)

        pageManager.finishPage()

        val file = File(ensurePdfDir(context), "comparison_${timestampFilename()}.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }
}
