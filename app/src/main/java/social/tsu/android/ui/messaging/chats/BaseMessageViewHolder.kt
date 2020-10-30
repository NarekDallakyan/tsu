package social.tsu.android.ui.messaging.chats

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import social.tsu.android.R
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.Message

import social.tsu.android.service.IS_TEXT_COPY_ENABLED

import social.tsu.android.utils.hide
import social.tsu.android.utils.show

const val TIME_FORMAT = "HH:mm"
const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

interface MessageContentCallback {
    fun didTapHashtag(hashtag: String)
    fun didTapUsername(username: String)
}

abstract class BaseMessageViewHolder(
    itemView: View,
    private val callaback: MessageContentCallback
) : RecyclerView.ViewHolder(itemView) {

    protected var currentItem: Message? = null

    private var separator: TextView? = itemView.findViewById(R.id.separator_date)

    private var messageCommonContainer: ConstraintLayout? = itemView.findViewById(R.id.message_common_container)
    private var messageContainer: ConstraintLayout? = itemView.findViewById(R.id.text_container)
    private var messageText: TextView? = itemView.findViewById(R.id.chat_message_text)
    private var messageTime: TextView? = itemView.findViewById(R.id.message_time_view)

    private var imageContainer: ConstraintLayout? = itemView.findViewById(R.id.image_container)
    private var messageImage: ImageView? = itemView.findViewById(R.id.chat_message_image)
    private var imageTime: TextView? = itemView.findViewById(R.id.image_time_view)

    @SuppressLint("SetTextI18n")
    open fun bind(message: Message, nextMessage: Message) {
        this.currentItem = message

        val todayDate = LocalDate.now()
        val messageDate = message.createdAt.getDate(DATE_TIME_PATTERN)
        val prevMessageDate = nextMessage.createdAt.getDate(DATE_TIME_PATTERN)

        if (message.senderId != nextMessage.senderId) {
            messageCommonContainer?.setMargin(top = 8)
        } else {
            messageCommonContainer?.setMargin(top = 0)
        }

        when {
            messageDate.isAfter(prevMessageDate) || message.id == nextMessage.id -> {
                if (messageDate.isEqual(todayDate))
                    separator?.text = separator?.resources?.getString(R.string.today)
                else
                    separator?.text = "${messageDate.month.name.toLowerCase()
                        .capitalize()} ${messageDate.dayOfMonth}, ${messageDate.year}"

                messageCommonContainer?.setMargin(top = 0)
                separator?.visibility = View.VISIBLE
            }
            else -> {
                separator?.visibility = View.GONE
            }
        }

        if (IS_TEXT_COPY_ENABLED) {
            messageText?.setTextIsSelectable(true)
        }

        messageText?.let { textView ->
            if (message.text.isNotBlank()) {
                textView.show()

                val spannable = SpannableString(message.text)
                textView.text = TSUTextTokenizingHelper.tokenize(
                    itemView.context,
                    spannable,
                    callaback::didTapUsername,
                    callaback::didTapHashtag
                )
                textView.movementMethod = LinkMovementMethod.getInstance()
                messageTime?.text = message.createdAt.getTime(DATE_TIME_PATTERN, TIME_FORMAT)
            } else {
                textView.hide()
                messageContainer.hide()
            }
        }

        messageImage?.let { imageView ->
            if (!message.pictureUrl.isNullOrBlank()) {
                imageView.show()
                Glide.with(itemView)
                    .load(formatUrl(message))
                    .into(imageView)
                imageTime?.text = message.createdAt.getTime(DATE_TIME_PATTERN, TIME_FORMAT)
            } else {
                imageView.hide()
                imageContainer.hide()
            }
        }
    }

    private fun formatUrl(message: Message): String? {
        // Handle relative paths from the API_HOST
        val pictureUrl = message.pictureUrl ?: return null

        if (pictureUrl.startsWith("/")) {
            return "${HostProvider.imageHost}$pictureUrl"
        }

        return pictureUrl
    }

    private fun String.getTime(parserPattern: String, timePattern: String): String {
        return LocalDateTime
            .parse(this, DateTimeFormatter.ofPattern(parserPattern))
            .atOffset(ZoneOffset.UTC)
            .withOffsetSameInstant(OffsetDateTime.now().offset)
            .toOffsetTime()
            .format(DateTimeFormatter.ofPattern(timePattern))
    }

    private fun String.getDate(parserPattern: String): LocalDate {
        return LocalDateTime.parse(this, DateTimeFormatter.ofPattern(parserPattern))
            .toLocalDate()
    }

    fun View.setMargin(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
        val params = (layoutParams as? ViewGroup.MarginLayoutParams)
        params?.setMargins(
            left?.dpToPx(this.context) ?: params.leftMargin,
            top?.dpToPx(this.context) ?: params.topMargin,
            right?.dpToPx(this.context) ?: params.rightMargin,
            bottom?.dpToPx(this.context) ?: params.bottomMargin)
        layoutParams = params
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}