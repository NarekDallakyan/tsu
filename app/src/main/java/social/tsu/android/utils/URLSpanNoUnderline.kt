package social.tsu.android.utils

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import social.tsu.android.R


class URLSpanNoUnderline(
    private val context: Context,
    url: String?
) : ClickableSpan() {

    private val url: String? = if (url?.startsWith("http") == false) {
        "https://$url"
    } else url

    override fun onClick(widget: View) {
        context.openUrl(url)
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
        ds.color = ContextCompat.getColor(context, R.color.post_link_url)
    }

}