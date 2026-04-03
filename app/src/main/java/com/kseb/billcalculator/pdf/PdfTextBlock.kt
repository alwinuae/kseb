package com.kseb.billcalculator.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

object PdfTextBlock {

    fun drawText(
        pageManager: PdfPageManager,
        text: String,
        paint: Paint,
        alignment: TextAlignment = TextAlignment.LEFT
    ) {
        val lineHeight = paint.textSize + 4f
        val canvas = pageManager.checkPageBreak(lineHeight)
        val x = when (alignment) {
            TextAlignment.LEFT -> pageManager.getLeftMargin()
            TextAlignment.CENTER -> pageManager.getLeftMargin() + pageManager.getContentWidth() / 2f
            TextAlignment.RIGHT -> pageManager.getLeftMargin() + pageManager.getContentWidth()
        }
        val savedAlign = paint.textAlign
        paint.textAlign = when (alignment) {
            TextAlignment.LEFT -> Paint.Align.LEFT
            TextAlignment.CENTER -> Paint.Align.CENTER
            TextAlignment.RIGHT -> Paint.Align.RIGHT
        }
        canvas.drawText(text, x, pageManager.getY(), paint)
        paint.textAlign = savedAlign
        pageManager.advanceY(lineHeight)
    }

    fun drawKeyValue(
        pageManager: PdfPageManager,
        key: String,
        value: String,
        keyPaint: Paint,
        valuePaint: Paint
    ) {
        val lineHeight = maxOf(keyPaint.textSize, valuePaint.textSize) + 4f
        val canvas = pageManager.checkPageBreak(lineHeight)
        val y = pageManager.getY()

        val savedKeyAlign = keyPaint.textAlign
        val savedValueAlign = valuePaint.textAlign

        keyPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(key, pageManager.getLeftMargin(), y, keyPaint)

        valuePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            value,
            pageManager.getLeftMargin() + pageManager.getContentWidth(),
            y,
            valuePaint
        )

        keyPaint.textAlign = savedKeyAlign
        valuePaint.textAlign = savedValueAlign

        pageManager.advanceY(lineHeight)
    }

    fun drawDivider(pageManager: PdfPageManager) {
        val canvas = pageManager.checkPageBreak(6f)
        val y = pageManager.getY()
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(
            pageManager.getLeftMargin(),
            y,
            pageManager.getLeftMargin() + pageManager.getContentWidth(),
            y,
            linePaint
        )
        pageManager.advanceY(6f)
    }

    fun drawSpacer(pageManager: PdfPageManager, height: Float) {
        pageManager.checkPageBreak(height)
        pageManager.advanceY(height)
    }
}
