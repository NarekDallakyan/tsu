package social.tsu.android.ui.util

import android.graphics.drawable.RippleDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import social.tsu.android.R

interface HtmlAnchorClickListener {
    fun onHyperLinkClicked(name: String)
}

fun addClickableSpan(linkableTextView: TextView?, htmlString: String, listener: HtmlAnchorClickListener) {
    linkableTextView?.let {
        val sequence = HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
        Log.d("addClickableSpan", "sequence = $sequence")
        val spannableString = SpannableStringBuilder(sequence)
        val urls = spannableString.getSpans(0, sequence.length, URLSpan::class.java)
        urls.forEach { span ->
            with(spannableString) {
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                val flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                val linkColor = linkableTextView.context.getColor(R.color.colorPrimary)

                val clickable = object : ClickableSpan() {

                    override fun onClick(view: View) {
                        // Prevent CheckBox state from being toggled when link is clicked
                        linkableTextView.cancelPendingInputEvents()
                        removeRippleEffectFromCheckBox(linkableTextView)
                        listener.onHyperLinkClicked(span.url)
                    }

                    override fun updateDrawState(textPaint: TextPaint) {
                        textPaint.color = linkColor
                        textPaint.isUnderlineText = true
                    }
                }
                setSpan(clickable, start, end, flags)
                setSpan(ForegroundColorSpan(linkColor), start, end, flags)
                removeSpan(span)
            }

            with(it) {
                text = spannableString
                linksClickable = true
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }
}

fun removeRippleEffectFromCheckBox(textView: TextView) {
    var drawable = textView.background
    if (drawable is RippleDrawable) {
        drawable = drawable.findDrawableByLayerId(0)
        textView.background = drawable
    }
}