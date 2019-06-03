package com.glureau.textviewsandbox


import android.graphics.Rect
import android.text.TextPaint

/**
 * Files to describe different method to align ImageSpan with text.
 *
 * When you create an ImageSpan, you can specify the alignment
 * ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM) // Align with the text bottom (above the lowest point of the glyph in the font)
 * ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE) // Align with the text baseline
 */

open class TextBoundsDrawableHeightComputeMode(private val str: String, private val removeDescent: Boolean) :
    DrawableHeightComputeMode {
    private val bounds = Rect()
    override fun computeHeight(textPaint: TextPaint, text: CharSequence): Int {
        textPaint.getTextBounds(str, 0, str.length, bounds)
        if (removeDescent) {
            return bounds.height() - textPaint.fontMetricsInt.descent
        } else {
            return bounds.height()
        }
    }
}

class AscentDrawableHeightComputeMode : DrawableHeightComputeMode {
    override fun computeHeight(textPaint: TextPaint, text: CharSequence): Int {
        return -textPaint.fontMetricsInt.ascent
    }
}

class AllLettersTextBoundsDrawableHeightComputeMode :
    TextBoundsDrawableHeightComputeMode("ABCDEFGHIJKLMNOPDRSTUVWXYZabcdefghijklmnopqrstuvwxyz", true)

class CapsTextBoundsDrawableHeightComputeMode : TextBoundsDrawableHeightComputeMode("ABCDEFGHIJKLMNOPDRSTUVWXYZ", false)