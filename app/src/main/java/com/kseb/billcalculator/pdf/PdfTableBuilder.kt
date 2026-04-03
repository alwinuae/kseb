package com.kseb.billcalculator.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object PdfTableBuilder {

    private const val CELL_PADDING = 6f
    private const val ROW_HEIGHT = 22f
    private val HEADER_BG_COLOR = Color.rgb(230, 230, 230)

    fun drawTable(
        pageManager: PdfPageManager,
        headers: List<String>,
        rows: List<List<String>>,
        columnWidths: List<Float>,
        paint: Paint
    ) {
        val borderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        val fillPaint = Paint().apply {
            color = HEADER_BG_COLOR
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(paint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellTextPaint = Paint(paint)

        val tableLeft = pageManager.getLeftMargin()

        // Draw header row
        drawRow(
            pageManager = pageManager,
            cells = headers,
            columnWidths = columnWidths,
            tableLeft = tableLeft,
            textPaint = headerTextPaint,
            borderPaint = borderPaint,
            fillPaint = fillPaint
        )

        // Draw data rows
        for (row in rows) {
            drawRow(
                pageManager = pageManager,
                cells = row,
                columnWidths = columnWidths,
                tableLeft = tableLeft,
                textPaint = cellTextPaint,
                borderPaint = borderPaint,
                fillPaint = null
            )
        }
    }

    private fun drawRow(
        pageManager: PdfPageManager,
        cells: List<String>,
        columnWidths: List<Float>,
        tableLeft: Float,
        textPaint: Paint,
        borderPaint: Paint,
        fillPaint: Paint?
    ) {
        val canvas = pageManager.checkPageBreak(ROW_HEIGHT)
        val y = pageManager.getY()

        var x = tableLeft
        for (i in cells.indices) {
            val cellWidth = if (i < columnWidths.size) columnWidths[i] else 100f

            // Fill background if provided
            fillPaint?.let {
                canvas.drawRect(x, y - ROW_HEIGHT + 4f, x + cellWidth, y + 4f, it)
            }

            // Draw cell border
            canvas.drawRect(x, y - ROW_HEIGHT + 4f, x + cellWidth, y + 4f, borderPaint)

            // Draw cell text
            val savedAlign = textPaint.textAlign
            textPaint.textAlign = Paint.Align.LEFT

            // Truncate text if too wide
            val maxTextWidth = cellWidth - 2 * CELL_PADDING
            val displayText = truncateText(cells.getOrElse(i) { "" }, textPaint, maxTextWidth)
            canvas.drawText(displayText, x + CELL_PADDING, y, textPaint)

            textPaint.textAlign = savedAlign
            x += cellWidth
        }

        pageManager.advanceY(ROW_HEIGHT)
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return if (truncated.isEmpty()) text.take(3) else "$truncated..."
    }
}
