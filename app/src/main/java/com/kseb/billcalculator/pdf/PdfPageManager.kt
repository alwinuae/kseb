package com.kseb.billcalculator.pdf

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument

class PdfPageManager(private val pdfDocument: PdfDocument) {

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN_LEFT = 40f
        const val MARGIN_RIGHT = 40f
        const val MARGIN_TOP = 50f
        const val MARGIN_BOTTOM = 50f
    }

    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null
    private var pageNumber = 0
    private var cursorY = MARGIN_TOP

    fun startNewPage(): Canvas {
        finishPage()
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        currentPage = page
        currentCanvas = page.canvas
        cursorY = MARGIN_TOP
        return page.canvas
    }

    fun getCurrentCanvas(): Canvas {
        return currentCanvas ?: startNewPage()
    }

    fun getY(): Float = cursorY

    fun setY(y: Float) {
        cursorY = y
    }

    fun advanceY(amount: Float) {
        cursorY += amount
    }

    fun checkPageBreak(requiredHeight: Float): Canvas {
        if (currentCanvas == null) {
            return startNewPage()
        }
        if (cursorY + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
            return startNewPage()
        }
        return currentCanvas!!
    }

    fun finishPage() {
        currentPage?.let { page ->
            pdfDocument.finishPage(page)
        }
        currentPage = null
        currentCanvas = null
    }

    fun getContentWidth(): Float = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    fun getLeftMargin(): Float = MARGIN_LEFT

    fun getPageNumber(): Int = pageNumber
}
