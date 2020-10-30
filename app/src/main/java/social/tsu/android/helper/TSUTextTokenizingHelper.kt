package social.tsu.android.helper

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.util.Patterns
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.toSpannable
import social.tsu.android.R
import social.tsu.android.utils.TSURegex
import social.tsu.android.utils.URLSpanNoUnderline
import java.util.regex.Pattern


object TSUTextTokenizingHelper {

    fun boldify(context: Context?, text: String, input: SpannableString): SpannableString {
        if (context == null || text.isEmpty()) return input

        val spanStart = input.indexOf(text)
        if (spanStart < 0) return input

        input.setSpan(
            TextAppearanceSpan(context, R.style.TSUBoldPostTextStyle),
            spanStart,
            spanStart + text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return input
    }

    fun boldifySymbol(context: Context?, input: SpannableString, symbol: Char) {
        if (context == null) return

        val start = input.indexOf(symbol)
        if (start >= 0) {
            input.setSpan(
                TextAppearanceSpan(context, R.style.TSUTextAppearance_Bold),
                start,
                start + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun makeCommunityCounter(
        context: Context?,
        count: Int,
        text: String,
        secondCount: Int
    ): Spannable? {
        if (context == null) return null

        val builder = SpannableStringBuilder()
            .append(
                count.toString(),
                ForegroundColorSpan(context.getColor(R.color.community_count_color)),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            .append("  ")
            .append(
                text,
                ForegroundColorSpan(context.getColor(R.color.community_text_color)),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        if (secondCount > 0) {
            builder.append(" ")
            if (secondCount > 9) {
                builder.append(
                    "9+", ForegroundColorSpan(context.getColor(R.color.community_count_color)),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                builder.append(
                    secondCount.toString(),
                    ForegroundColorSpan(context.getColor(R.color.community_second_count_color)),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return builder.toSpannable()
    }

    fun makeCommunityTitle(
        context: Context?,
        text: String,
        secondText: String
    ): Spannable? {
        if (context == null) return null

        val builder = SpannableStringBuilder()
            .append(
                text
            )
            .append("  ")
            .append(
                secondText,
                ForegroundColorSpan(context.getColor(R.color.community_moderated_color)),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        return builder.toSpannable()
    }

    fun clickable(
        context: Context?,
        input: SpannableString?,
        text: String?,
        textStyle: TsuClickableTextStyle = TsuClickableTextStyle.BOLD,
        onClick: () -> Unit
    ) {
        if (context == null || input == null || text.isNullOrBlank()) return

        val start = input.indexOf(text)
        if (start >= 0) {
            input.setSpan(
                TSUClickableSpan(context, onClick, textStyle),
                start,
                start + text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun tokenize(
        context: Context?,
        input: SpannableString,
        onClickUsername: ((String) -> Unit)?,
        onClickHashtag: ((String) -> Unit)?
    ): SpannableString {
        var output = input
        val context = context ?: return output

        val matcher = Pattern.compile(TSURegex.HASHTAG).matcher(input)
        while (matcher.find()) {
            val hashtag = matcher.group()
            val value = if (hashtag.startsWith('#')) hashtag.substringAfter('#') else hashtag
            output.setSpan(
                TSUClickableSpan(
                    context,
                    { onClickHashtag?.invoke(value) },
                    TsuClickableTextStyle.HASHTAG
                ),
                matcher.start(),
                matcher.end(),
                0
            )
        }

        val user = Pattern.compile(TSURegex.USERNAME).matcher(input)
        while (user.find()) {
            val username = user.group()
            val value = if (username.startsWith('@')) username.substringAfter('@') else username
            output.setSpan(
                TSUClickableSpan(
                    context,
                    { onClickUsername?.invoke(value) },
                    TsuClickableTextStyle.NORMAL_BLUE
                ),
                user.start(),
                user.end(),
                0
            )
        }

        val urlMatcher = Patterns.WEB_URL.matcher(input)
        while (urlMatcher.find()) {
            output.setSpan(
                URLSpanNoUnderline(context, urlMatcher.group()),
                urlMatcher.start(),
                urlMatcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return output
    }

    fun normalize(context: Context?, input: String): SpannableString {
        var output = SpannableString(input)
        val context = context ?: return output
        output.setSpan(
            TextAppearanceSpan(context, R.style.TSUDefaultPostTextStyle),
            0,
            input.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return output
    }

    private class TSUClickableSpan(
        private val context: Context,
        private val onClickText: () -> Unit,
        private val clickableTextStyle: TsuClickableTextStyle = TsuClickableTextStyle.BOLD
    ) : ClickableSpan() {

        override fun onClick(widget: View) {
            onClickText()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            when (clickableTextStyle) {
                TsuClickableTextStyle.BOLD -> {
                    ds.isUnderlineText = false
                    ds.color = Color.WHITE
                    ds.typeface = ResourcesCompat.getFont(context, R.font.lato_bold)
                }
                TsuClickableTextStyle.NORMAL -> {
                    ds.isUnderlineText = false
                    ds.color = ContextCompat.getColor(context, R.color.secondaryDarkGray)
                    ds.typeface = Typeface.create("lato", Typeface.NORMAL)
                }
                TsuClickableTextStyle.LINK -> {
                    ds.isUnderlineText = true
                    ds.color = ContextCompat.getColor(context, R.color.post_link_url)
                    ds.typeface = ResourcesCompat.getFont(context, R.font.lato_bold)
                }
                TsuClickableTextStyle.HASHTAG -> {
                    ds.isUnderlineText = false
                    ds.color = ContextCompat.getColor(context, R.color.post_link_url)
                    ds.typeface = Typeface.create("lato", Typeface.NORMAL)
                }
                TsuClickableTextStyle.NORMAL_BLUE -> {
                    ds.isUnderlineText = false
                    ds.color = ContextCompat.getColor(context, R.color.secondaryBlue)
                    ds.typeface = Typeface.create("lato", Typeface.NORMAL)
                }
            }
        }
    }

    enum class TsuClickableTextStyle {
        BOLD, NORMAL, LINK, HASHTAG, NORMAL_BLUE
    }

}