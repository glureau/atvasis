package com.glureau.textviewsandbox


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.SpannedString
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView

class DebugTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        private const val DEBUG_DRAWING = true
    }

    private val debugPaintTop = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
    private val debugPaintAscent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW }
    private val debugPaintBaseline = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val debugPaintDescent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLUE }
    private val debugPaintBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN }
    private val debugPaintText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        alpha = 120
        textSize = 102f
        // TODO: Manage center alignment...
    }
    private val tmpBounds = Rect()

    override fun onDraw(canvas: Canvas) {
        if (DEBUG_DRAWING) {
            /*
            Log.e("DRAW TEXT", "txt='$text' width=$width height=$height")
            debugPaintText.apply { textSize = this@DebugTextView.textSize }
            canvas.drawText(
                text.toString().replace("<tag>", "   ", true).toUpperCase(),
                compoundPaddingStart.toFloat(),
                baseline.toFloat(),
                debugPaintText
            )*/
        }
        super.onDraw(canvas)
        if (DEBUG_DRAWING) {
            /*
            if (text is SpannedString) {
                Log.e("DRAW ME", "text=${text.javaClass}")
                text.forEach {
                    paint.getTextBounds(it + "", 0, 1, tmpBounds)
                    Log.e("DRAW ME", "char=$it $tmpBounds")
                    //val finalPosY = baseline + posY * (baseline.toFloat() / height)
                    //canvas.drawRect(tmpBounds, debugPaintBaseline)
                }
            }*/
            Log.e("DRAW ME", "paint.fontMetrics = ${paint.fontMetricsInt}")
            canvas.drawLine(0f, baseline.toFloat(), width.toFloat(), baseline.toFloat(), debugPaintText)
            drawFontMetricsLine(canvas, paint.fontMetrics.top, debugPaintTop)
            drawFontMetricsLine(canvas, paint.fontMetrics.ascent, debugPaintAscent)
            drawFontMetricsLine(canvas, paint.fontMetrics.leading, debugPaintBaseline)
            drawFontMetricsLine(canvas, paint.fontMetrics.descent, debugPaintDescent)
            drawFontMetricsLine(canvas, paint.fontMetrics.bottom, debugPaintBottom)
            invalidate()
        }
    }

    private fun drawFontMetricsLine(
        canvas: Canvas,
        posY: Float,
        debugPaint: Paint
    ) {
        val finalPosY = baseline + posY * (baseline.toFloat() / height)
        Log.e(
            "DRAW ME ",
            "finalPosY: 0 < $finalPosY < $height // baseline=$baseline // textSize=$textSize"
        )
        canvas.drawLine(0f, finalPosY, width.toFloat(), finalPosY, debugPaint)
    }
}