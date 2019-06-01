package com.glureau.textviewsandbox

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG1 = ":droid1:"
        const val TAG2 = ":droid2:"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv1.testWith("Hello :droid1: World!")
        tv2.testWith("Hello :droid1: World!")
        tv3.testWith("Hello World! :droid2:")
        tv4.testWith("Hello :droid1: World! This is a damn long :droid2: text just to check it's ok")
        tv5.testWith("Hello :droid2: World!")
        tv6.testWith("Hello:droid1: 2 lines!")
    }

    private fun TextView.testWith(txt: String) {
        text = txt
        replaceTags()
        doOnPreDraw {
            adjustSizeToFit()
        }
    }

    private fun TextView.replaceTags() {
        replaceTagWithDrawable(TAG1, { drawable1() })
        replaceTagWithDrawable(TAG2, { drawable2() })
    }

    fun TextView.replaceTagWithDrawable(tag: String, drawableFactory: () -> Drawable, ignoreCase: Boolean = true) {
        val tagPos = text.indexOf(tag, ignoreCase = ignoreCase)
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

    fun drawable1(): Drawable = ContextCompat.getDrawable(this, R.drawable.droid1)!!.apply {
        //Log.e(TAG, "Load drawable (intrinsicWidth=$intrinsicWidth intrinsicHeight=$intrinsicHeight)")
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    fun drawable2(): Drawable = ContextCompat.getDrawable(this, R.drawable.droid2)!!.apply {
        //Log.e(TAG, "Load drawable (intrinsicWidth=$intrinsicWidth intrinsicHeight=$intrinsicHeight)")
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }
}
