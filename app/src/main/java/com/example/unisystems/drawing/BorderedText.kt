package com.example.unisystems.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface


class BorderedText(interiorColor: Int, exteriorColor: Int, textSize: Float) {
    private val interiorPaint: Paint = Paint()
    private val exteriorPaint: Paint
    val textSize: Float

    constructor(textSize: Float) : this(
        Color.WHITE,
        Color.BLACK,
        textSize
    )

    fun setTypeface(typeface: Typeface?) {
        interiorPaint.typeface = typeface
        exteriorPaint.typeface = typeface
    }

    fun drawText(
        canvas: Canvas,
        posX: Float,
        posY: Float,
        text: String?
    ) {
        canvas.drawText(text!!, posX, posY, exteriorPaint)
        canvas.drawText(text, posX, posY, interiorPaint)
    }

    fun drawText(
        canvas: Canvas,
        posX: Float,
        posY: Float,
        text: String?,
        bgPaint: Paint?
    ) {
        val width = exteriorPaint.measureText(text)
        val textSize = exteriorPaint.textSize
        val paint = Paint(bgPaint)
        paint.style = Paint.Style.FILL
        paint.alpha = 160
        canvas.drawRect(posX, posY + textSize.toInt(), posX + width.toInt(), posY, paint)
        canvas.drawText(text!!, posX, posY + textSize, interiorPaint)
    }

    init {
        interiorPaint.textSize = textSize
        interiorPaint.color = interiorColor
        interiorPaint.style = Paint.Style.FILL
        interiorPaint.isAntiAlias = false
        interiorPaint.alpha = 255
        exteriorPaint = Paint()
        exteriorPaint.textSize = textSize
        exteriorPaint.color = exteriorColor
        exteriorPaint.style = Paint.Style.FILL_AND_STROKE
        exteriorPaint.strokeWidth = textSize / 8
        exteriorPaint.isAntiAlias = false
        exteriorPaint.alpha = 255
        this.textSize = textSize
    }
}
