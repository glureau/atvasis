package com.glureau.textviewsandbox

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import kotlinx.android.synthetic.main.activity_medium.*

class MediumActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium)

        tv1.adjustSizeToFit()
        tv2.adjustSizeToFit()
        tv3.adjustSizeToFit()
        tv3.text = "FOR <picto> DEVS"
        val drawable = ContextCompat.getDrawable(this, R.drawable.medium_logo_lowres)!!.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
        tv3.replaceTagWithDrawable("<picto>", { drawable })
    }


    private fun TextView.replaceTagWithDrawable(
        tag: String,
        drawableFactory: () -> Drawable,
        ignoreCase: Boolean = true
    ) {
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
}