package com.glureau.textviewsandbox

import android.annotation.SuppressLint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.*
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.truncate

private const val LOG_TAG = "TextViewAutosizeExt"
private const val VERY_WIDE = 1024 * 1024

private var tempTextPaint: TextPaint? = null

fun TextView.replaceTagWithDrawable(tag: String, drawableFactory: () -> Drawable) {
    val tagPos = text.indexOf(tag)
    if (tagPos >= 0) {
        var spannableString = text as? SpannableString?
        if (spannableString == null) {
            spannableString = SpannableString(text)
        }
        spannableString.setSpan(
            ImageSpan(drawableFactory(), ImageSpan.ALIGN_BASELINE),
            tagPos,
            tagPos + tag.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        text = spannableString
    }
}

@SuppressLint("RestrictedApi", "WrongConstant")
fun TextView.adjustSizeToFit() {
    if (this !is AppCompatTextView) throw java.lang.IllegalStateException("You have to use AppCompatTextView to use adjustSizeToFit")
    if (autoSizeTextAvailableSizes().size < 0) throw java.lang.IllegalStateException("You have to define autosize attributes")

    // Force enable the autosize so the base (AppCompat)TextView will generate auto text sizes
    // downcast required for <26 api
    (this as AppCompatTextView).setAutoSizeTextTypeUniformWithConfiguration(
        safeAutoSizeMinTextSize(), safeAutoSizeMaxTextSize(),
        safeAutoSizeStepGranularity(), TypedValue.COMPLEX_UNIT_PX
    )

    val bestSize = findLargestTextSizeWhichFitsWidth(width.toFloat())

    // Force disable autosize as we'll override the default behaviour by setting the size manually.
    (this as AppCompatTextView).setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)

    Log.e(LOG_TAG, "Selected font size: $bestSize")
    setTextSize(TypedValue.COMPLEX_UNIT_PX, bestSize.toFloat())

    // Requires a last update with final textPaint (edge case when the last try is too big)
    alignImageToText(paint)
}

fun TextView.alignImageToText(textPaint: TextPaint) {
    (text as? SpannedString)?.getSpans(0, text.length, DynamicDrawableSpan::class.java)?.forEach {
        val drawable = it.drawable
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
        val fontHeight = -textPaint.fontMetricsInt.ascent
        val drawableWidth = truncate(fontHeight * ratio).toInt()
        drawable.setBounds(0, 0, drawableWidth, fontHeight)
    }
}

@SuppressLint("RestrictedApi")
private fun AppCompatTextView.safeAutoSizeMinTextSize(): Int =
    if (autoSizeMinTextSize != -1) autoSizeMinTextSize
    else TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics).toInt()

@SuppressLint("RestrictedApi")
private fun AppCompatTextView.safeAutoSizeMaxTextSize() =
    if (autoSizeMaxTextSize != -1) autoSizeMaxTextSize
    else TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 112f, resources.displayMetrics).toInt()

@SuppressLint("RestrictedApi")
private fun AppCompatTextView.safeAutoSizeStepGranularity() =
    if (autoSizeStepGranularity != -1) autoSizeStepGranularity else 1


/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper.
 * Performs a binary search to find the largest text size that will still fit within the size
 * available to this view.
 **********************************************************************************************************************/
private fun AppCompatTextView.findLargestTextSizeWhichFitsWidth(availableWidth: Float): Int {
    val autoSizeTextAvailableSizes = autoSizeTextAvailableSizes()
    val sizesCount = autoSizeTextAvailableSizes.size
    if (sizesCount == 0) {
        throw IllegalStateException("No available text sizes to choose from.")
    }

    var bestSizeIndex = 0
    var lowIndex = bestSizeIndex + 1
    var highIndex = sizesCount - 1
    var sizeToTryIndex: Int
    while (lowIndex <= highIndex) {
        sizeToTryIndex = (lowIndex + highIndex) / 2
        if (suggestedSizeFitsInWidth(autoSizeTextAvailableSizes[sizeToTryIndex], availableWidth)) {
            bestSizeIndex = lowIndex
            lowIndex = sizeToTryIndex + 1
        } else {
            highIndex = sizeToTryIndex - 1
            bestSizeIndex = highIndex
        }
    }

    return autoSizeTextAvailableSizes[bestSizeIndex]
}

/**********************************************************************************************************************
 * Shortcuts to suggestedSizeFitsInSpace(suggestedSizeInPx, RectF(0, 0, availableWidth, VERY_WIDE))
 **********************************************************************************************************************/
private fun AppCompatTextView.suggestedSizeFitsInWidth(suggestedSizeInPx: Int, availableWidth: Float) =
    suggestedSizeFitsInSpace(suggestedSizeInPx, RectF(0f, 0f, availableWidth, VERY_WIDE.toFloat()))

/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper.
 **********************************************************************************************************************/
private fun AppCompatTextView.suggestedSizeFitsInSpace(suggestedSizeInPx: Int, availableSpace: RectF): Boolean {
    transformationMethod?.let {
        val transformedText = it.getTransformation(text, this)
        if (transformedText != null) {
            text = transformedText
        }
    }

    val maxLines = if (Build.VERSION.SDK_INT >= 16) maxLines else -1

    tempTextPaint?.reset()
    if (tempTextPaint == null) {
        tempTextPaint = TextPaint()
    }
    tempTextPaint?.let {
        it.set(paint)
        it.textSize = suggestedSizeInPx.toFloat()
    }
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    tempTextPaint?.let { alignImageToText(it) }
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////
    //////////////////////////////////////////////////////

    // Needs reflection call due to being private.
    val alignment = invokeAndReturnWithDefault(
        this, "getLayoutAlignment", Layout.Alignment.ALIGN_NORMAL
    )
    val layout = if (Build.VERSION.SDK_INT >= 23)
        createStaticLayoutForMeasuring(
            text, alignment, Math.round(availableSpace.right), maxLines
        )
    else
        createStaticLayoutForMeasuringPre23(
            text, alignment, Math.round(availableSpace.right)
        )
    // Lines overflow.
    if (maxLines != -1 && (layout.lineCount > maxLines || layout.getLineEnd(layout.lineCount - 1) != text.length)) {
        Log.e(
            LOG_TAG, "Lines overflow $suggestedSizeInPx -> ${layout.lineCount} > $maxLines   OR   " +
                    layout.getLineEnd(layout.lineCount - 1) + " != " + text.length
        )
        return false
    }

    // Height overflow.
    if (layout.height > availableSpace.bottom) {
        Log.e(LOG_TAG, "Line height overflow? $suggestedSizeInPx -> " + layout.height + "<=" + availableSpace.bottom)
    } else {
        Log.e(LOG_TAG, "No overflow for suggestedSizeInPx=$suggestedSizeInPx")
    }
    return layout.height <= availableSpace.bottom

}

/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper.
 **********************************************************************************************************************/

@RequiresApi(23)
private fun AppCompatTextView.createStaticLayoutForMeasuring(
    text: CharSequence,
    alignment: Layout.Alignment?, availableWidth: Int, maxLines: Int
): StaticLayout {
    // Can use the StaticLayout.Builder (along with TextView params added in or after
    // API 23) to construct the layout.
    val textDirectionHeuristic = invokeAndReturnWithDefault<TextDirectionHeuristic>(
        this, "getTextDirectionHeuristic",
        TextDirectionHeuristics.FIRSTSTRONG_LTR
    )

    val layoutBuilder = StaticLayout.Builder.obtain(
        text, 0, text.length, tempTextPaint, availableWidth
    )

    return layoutBuilder.setAlignment(alignment!!)
        .setLineSpacing(
            lineSpacingExtra,
            lineSpacingMultiplier
        )
        .setIncludePad(includeFontPadding)
        .setBreakStrategy(breakStrategy)
        .setHyphenationFrequency(hyphenationFrequency)
        .setMaxLines(if (maxLines == -1) Integer.MAX_VALUE else maxLines)
        .setTextDirection(textDirectionHeuristic)
        .build()
}

/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper.
 **********************************************************************************************************************/

private fun AppCompatTextView.createStaticLayoutForMeasuringPre23(
    text: CharSequence,
    alignment: Layout.Alignment?, availableWidth: Int
): StaticLayout {
    // Setup defaults.
    var lineSpacingMultiplier = 1.0f
    var lineSpacingAdd = 0.0f
    var includePad = true

    if (Build.VERSION.SDK_INT >= 16) {
        // Call public methods.
        lineSpacingMultiplier = getLineSpacingMultiplier()
        lineSpacingAdd = lineSpacingExtra
        includePad = includeFontPadding
    } else {
        // Call private methods and make sure to provide fallback defaults in case something
        // goes wrong. The default values have been inlined with the StaticLayout defaults.
        lineSpacingMultiplier = invokeAndReturnWithDefault(
            this,
            "getLineSpacingMultiplier", lineSpacingMultiplier
        )
        lineSpacingAdd = invokeAndReturnWithDefault(
            this,
            "getLineSpacingExtra", lineSpacingAdd
        )
        includePad = invokeAndReturnWithDefault(
            this,
            "getIncludeFontPadding", includePad
        )
    }

    // The layout could not be constructed using the builder so fall back to the
    // most broad constructor.
    return StaticLayout(
        text, tempTextPaint, availableWidth,
        alignment,
        lineSpacingMultiplier,
        lineSpacingAdd,
        includePad
    )
}

/**********************************************************************************************************************
 * Remapping auto-size functionalities from AppCompatTextViewAutoSizeHelper.
 **********************************************************************************************************************/
fun AppCompatTextView.autoSizeTextAvailableSizes(): IntArray =
    invokeAndReturnWithDefault(this, "getAutoSizeTextAvailableSizes", IntArray(0))


/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper but adapted to AppCompatTextView.
 **********************************************************************************************************************/

private fun <T> invokeAndReturnWithDefault(target: Any, methodName: String, defaultValue: T): T {
    return try {
        // Cache lookup.
        getTextViewMethod(methodName)!!.invoke(target) as T
    } catch (ex: Exception) {
        Log.w(LOG_TAG, "Failed to invoke TextView#$methodName() method", ex)
        defaultValue
    }
}


/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper but adapted to AppCompatTextView.
 **********************************************************************************************************************/

// Cache of TextView methods used via reflection; the key is the method name and the value is
// the method itself or null if it can not be found.
private val sTextViewMethodByNameCache = ConcurrentHashMap<String, Method>()

private fun getTextViewMethod(methodName: String): Method? {
    try {
        var method: Method? = sTextViewMethodByNameCache[methodName]
        if (method == null) {
            method = AppCompatTextView::class.java.getDeclaredMethod(methodName)
            if (method != null) {
                method.isAccessible = true
                // Cache update.
                sTextViewMethodByNameCache[methodName] = method
            }
        }

        return method
    } catch (ex: Exception) {
        Log.w(LOG_TAG, "Failed to retrieve TextView#$methodName() method", ex)
        return null
    }
}