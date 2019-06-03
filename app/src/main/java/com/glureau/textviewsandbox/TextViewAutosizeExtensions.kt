package com.glureau.textviewsandbox

import android.annotation.SuppressLint
import android.graphics.RectF
import android.os.Build
import android.text.*
import android.text.style.DynamicDrawableSpan
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.doOnPreDraw
import androidx.core.widget.TextViewCompat
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.truncate

private const val LOG_TAG = "TextViewAutosizeExt"
private const val VERY_WIDE = 1024 * 1024

private var tempTextPaint: TextPaint? = null

interface DrawableHeightComputeMode {
    fun computeHeight(textPaint: TextPaint, text: CharSequence): Int
}


/**
 * Method to adjust (auto-size) the text size AND the images height.
 * This approach is based on width and lines instead of height, so you can use layout_heigt="wrap_content"
 * and the TextView height will adapt to the best font size (instead of hardcoded heights).
 * Idea of improvements: use layout_height value if hardcoded, and use current behaviour for match_parent or wrap_content.
 *
 * /!\  WARNING  /!\
 * You can define the min/max/granularity of autoSize with the standard appCompat definitions BUT you have to define the type to uniform.
 *         app:autoSizeTextType="uniform"
 *         app:autoSizeMaxTextSize="500sp"
 * If you don't define type=uniform, the XML values will be ignored.
 * /!\  WARNING  /!\
 *
 * Also you can use this method with no autoSize parameters and it will work with the default values.
 */
@SuppressLint("RestrictedApi", "WrongConstant")
fun TextView.adjustSizeToFit(drawableHeightCompute: DrawableHeightComputeMode = AllLettersTextBoundsDrawableHeightComputeMode()) {
    if (this !is AppCompatTextView) throw java.lang.IllegalStateException("You have to use AppCompatTextView to use adjustSizeToFit")

    // Wait pre-draw to ensure the view width is ready
    doOnPreDraw {
        // Re-store previous configuration or create new one based on XML attributes
        val autoSizeConfiguration = providesAutoSizeConfiguration()

        // Force enable the autosize so the base (AppCompat)TextView will generate auto text sizes (will use default values, not XML ones)
        // downcast required for <26 api (will target hidden API)
        (this as AppCompatTextView).setAutoSizeTextTypeUniformWithConfiguration(
            safeAutoSizeMinTextSize(autoSizeConfiguration), safeAutoSizeMaxTextSize(autoSizeConfiguration),
            safeAutoSizeStepGranularity(autoSizeConfiguration), TypedValue.COMPLEX_UNIT_PX
        )

        val bestSize = findLargestTextSizeWhichFitsWidth(
            width.toFloat() - totalPaddingStart - totalPaddingEnd,
            drawableHeightCompute
        )

        // Force disable autosize as we'll override the default behaviour by setting the size manually.
        // downcast required for <26 api (will target hidden API)
        (this as AppCompatTextView).setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)

        setTextSize(TypedValue.COMPLEX_UNIT_PX, bestSize.toFloat())

        // Requires a last update with final textPaint (edge case when the last try is too big)
        alignImageToText(paint, drawableHeightCompute)

        // As we've probably changed the height, we need to request layout update.
        requestLayout()
    }
}

fun TextView.alignImageToText(textPaint: TextPaint, drawableHeightComputer: DrawableHeightComputeMode) {
    (text as? SpannedString)?.getSpans(0, text.length, DynamicDrawableSpan::class.java)?.forEach {
        val drawable = it.drawable
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
        val fontHeight = drawableHeightComputer.computeHeight(textPaint, text)
        val drawableWidth = truncate(fontHeight * ratio).toInt()
        drawable.setBounds(0, 0, drawableWidth, fontHeight)
    }
}


/**********************************************************************************************************************
 * Storage of AutoSizeConfiguration
 *
 * 1 - We cannot setTextSize without disabling autosize (or use Reflection but way more fragile...)
 * 2 - Disabling autosize reset all values and attrs is not re-parsed so data is lost.
 * Due to these 2 reasons, we need a way to keep track of the previous values to restore them when required.
 *
 * The solution is to store in a static map a weak reference of the view and an AutoSizeConfiguration data class.
 * So if there is no data available (first usage or first view destroyed and recreated),
 * we store the attributes in our data class, and if there is already a data class, we re-use them.
 **********************************************************************************************************************/

private val autoSizeConfigurations = mutableMapOf<WeakReference<AppCompatTextView>, AutoSizeConfiguration>()

private data class AutoSizeConfiguration(val minTextSize: Int, val maxTextSize: Int, val stepGranularity: Int)

@SuppressLint("RestrictedApi")
private fun AppCompatTextView.providesAutoSizeConfiguration(): AutoSizeConfiguration {
    val previousConfigurations = autoSizeConfigurations.filterKeys { it.get() === this }
    return if (previousConfigurations.isNotEmpty()) {
        previousConfigurations.values.toList()[0]
    } else {
        AutoSizeConfiguration(autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity)
            .also { autoSizeConfigurations[WeakReference(this)] = it }
    }
}

// Values copied from AppCompatTextViewAutoSizeHelper
// Default minimum size for auto-sizing text in scaled pixels.
private const val DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP = 12f
// Default maximum size for auto-sizing text in scaled pixels.
private const val DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP = 112f
// Default value for the step size in pixels.
private const val DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX = 1

private fun AppCompatTextView.safeAutoSizeMinTextSize(previousConfiguration: AutoSizeConfiguration): Int =
    if (previousConfiguration.minTextSize != -1) previousConfiguration.minTextSize
    else TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP,
        resources.displayMetrics
    ).toInt()

private fun AppCompatTextView.safeAutoSizeMaxTextSize(previousConfiguration: AutoSizeConfiguration) =
    if (previousConfiguration.maxTextSize != -1) previousConfiguration.maxTextSize
    else TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP,
        resources.displayMetrics
    ).toInt()

private fun safeAutoSizeStepGranularity(previousConfiguration: AutoSizeConfiguration) =
    if (previousConfiguration.stepGranularity != -1) previousConfiguration.stepGranularity
    else DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX


/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper.
 * Performs a binary search to find the largest text size that will still fit within the size
 * available to this view.
 **********************************************************************************************************************/
private fun AppCompatTextView.findLargestTextSizeWhichFitsWidth(
    availableWidth: Float,
    drawableHeightCompute: DrawableHeightComputeMode
): Int {
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
        if (suggestedSizeFitsInWidth(
                autoSizeTextAvailableSizes[sizeToTryIndex],
                availableWidth,
                drawableHeightCompute
            )
        ) {
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
private fun AppCompatTextView.suggestedSizeFitsInWidth(
    suggestedSizeInPx: Int,
    availableWidth: Float,
    drawableHeightCompute: DrawableHeightComputeMode
) =
    suggestedSizeFitsInSpace(
        suggestedSizeInPx,
        RectF(0f, 0f, availableWidth, VERY_WIDE.toFloat()),
        drawableHeightCompute
    )

/**********************************************************************************************************************
 * Copied from AppCompatTextViewAutoSizeHelper + modifications for image alignment (see comment block)
 **********************************************************************************************************************/
private fun AppCompatTextView.suggestedSizeFitsInSpace(
    suggestedSizeInPx: Int,
    availableSpace: RectF,
    drawableHeightCompute: DrawableHeightComputeMode
): Boolean {
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
    tempTextPaint?.let { alignImageToText(it, drawableHeightCompute) }
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
    val layout = if (Build.VERSION.SDK_INT >= 23) {
        createStaticLayoutForMeasuring(text, alignment, Math.round(availableSpace.right), maxLines)
    } else {
        createStaticLayoutForMeasuringPre23(text, alignment, Math.round(availableSpace.right))
    }

    // Lines overflow.
    if (maxLines != -1 && (layout.lineCount > maxLines || layout.getLineEnd(layout.lineCount - 1) != text.length)) {
        return false
    }

    // Height overflow.
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
        text, 0, text.length, tempTextPaint!!, availableWidth
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