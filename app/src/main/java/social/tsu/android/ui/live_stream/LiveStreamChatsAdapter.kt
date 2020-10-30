package social.tsu.android.ui.live_stream


import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import kotlinx.android.synthetic.main.item_live_message_incoming.view.*
import social.tsu.android.R
import social.tsu.android.data.local.models.ChatMessageData
import social.tsu.android.data.local.models.ChatUserData
import social.tsu.android.helper.Constants
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.api.HostProvider
import social.tsu.android.service.IS_TEXT_COPY_ENABLED
import social.tsu.android.ui.messaging.chats.MessageContentCallback
import social.tsu.android.ui.util.BaseViewHolder
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import java.util.*
import kotlin.math.roundToInt

class LiveStreamChatsAdapter(
    private val context: Context,
    private val myChatsUser: User,
    private val callback: LiveStreamMessageContentCallback
) : RecyclerView.Adapter<LiveMessageViewHolder>() {

    private val messages = arrayListOf<LiveStreamMessage>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveMessageViewHolder {
        return LiveMessageViewHolder.create(parent, callback)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun addMessage(message: Message) {
        if (messages.any { it.streamMessage?.id == message.id }) return
        addMessage(
            LiveStreamMessage(
                message.user,
                message.text,
                message.user.id == myChatsUser.id, message
            )
        )
    }

    fun deleteMessage(message: Message) {
        val index = messages.indexOfFirst { it.streamMessage?.id == message.id }
        if (index < 0) return
        messages.removeAt(index)
        notifyItemRemoved(index)
    }

    private fun addMessage(liveStreamMessage: LiveStreamMessage) {
        messages.add(liveStreamMessage)
        notifyItemInserted(messages.size - 1)
    }

    fun newUserJoined(user: User) {
        val fullname = user.extraData[ChatUserData.NAME_KEY] as? String
        fullname?.let {
            addMessage(
                LiveStreamMessage(
                    user,
                    context.getString(
                        R.string.join_stream_message
                    ),
                    myChatsUser.id == user.id
                )
            )
        }

    }

    override fun onBindViewHolder(holder: LiveMessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    interface LiveStreamMessageContentCallback : MessageContentCallback {
        fun onIncomingMessageLongPress(message: Message)
        fun onOutgoingMessageLongPress(message: Message)
        fun onUserTapped(userId: Int)
    }

}


class LiveMessageViewHolder(
    view: View,
    private val callback: LiveStreamChatsAdapter.LiveStreamMessageContentCallback
) : BaseViewHolder(view) {

    private var messageText: TextView? = itemView.chat_message_text
    private var chatProfileImage: CircleImageView? =
        itemView.chat_message_profile_image

    override fun <T> bind(item: T) {

        val message = item as LiveStreamMessage
        message.user.devices
        val userName: String = "" + message.user.extraData[ChatUserData.NAME_KEY] as? String
        val verifyStatus: Double? = message.user.extraData[ChatUserData.VERIFIED_STATUS] as? Double
        if (IS_TEXT_COPY_ENABLED) {
            messageText?.setTextIsSelectable(true)
        }
        messageText?.let { textView ->
            if (message.messageText.isNotEmpty()) {
                textView.show()
                val spannable = SpannableString(message.messageText)
                var chatMessage: String = "" + TSUTextTokenizingHelper.tokenize(
                    itemView.context,
                    spannable,
                    callback::didTapUsername,
                    callback::didTapHashtag
                )
                var name = makeNameLetterCapital(userName)+ "\n"
                setClickableString(name, name + chatMessage, textView, message, verifyStatus)

            } else {
                textView.hide()
            }
        }

        chatProfileImage?.let { imageView ->
            val profilePicUrl = message.user.extraData[ChatUserData.AVATAR_URL_KEY] as? String
            if (profilePicUrl.isNullOrEmpty()) {
                imageView.setImageResource(R.drawable.user)
            } else {
                Glide.with(itemView)
                    .load(formatUrl(profilePicUrl))
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(imageView)
            }
            imageView.borderColor = ContextCompat.getColor(itemView.context, R.color.ib_fr_white)
            imageView.isBorderOverlay = true
            imageView.borderWidth = 2
        }

        chatProfileImage?.setOnClickListener {
            openProfile(message)
        }

        messageText?.setOnLongClickListener {
            message.streamMessage?.let { sm ->
                if (message.isMine) {
                    callback.onOutgoingMessageLongPress(sm)
                } else {
                    callback.onIncomingMessageLongPress(sm)
                }
            }
            return@setOnLongClickListener true
        }
    }

    private fun setClickableString(
        clickableValue: String,
        wholeValue: String,
        textView: TextView,
        message: LiveStreamMessage,
        verifyStatus: Double?
    ) {
        val spannableString = SpannableString(wholeValue)
        val startIndex = wholeValue.indexOf(clickableValue)
        val endIndex = startIndex + clickableValue.length
        val context = textView.context
        spannableString.setSpan(object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // <-- this will remove automatic underline in set span
                ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                ds.color = ContextCompat.getColor(context, R.color.white)
            }

            override fun onClick(widget: View) {
                // do what you want with clickable value
                openProfile(message)

            }
        }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        try {
            verifyStatus?.let {
                if (Constants.isVerified(verifyStatus.roundToInt())) {
                    val imageSpan = ImageSpan(textView.context, R.drawable.ic_verified_extra_small)
                    spannableString.setSpan(
                        imageSpan,
                        clickableValue.length - 2,
                        clickableValue.length + 1 - 2,
                        0
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        textView.text = spannableString
        textView.movementMethod =
            LinkMovementMethod.getInstance() // <-- important, onClick in ClickableSpan won't work without this
    }

    private fun formatUrl(pictureUrl: String): String? {
        // Handle relative paths from the API_HOST

        if (pictureUrl.startsWith("/")) {
            return "${HostProvider.imageHost}$pictureUrl"
        }

        return pictureUrl
    }

    companion object {

        fun create(
            parent: ViewGroup,
            callback: LiveStreamChatsAdapter.LiveStreamMessageContentCallback
        ): LiveMessageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_live_message_incoming, parent, false)
            return LiveMessageViewHolder(view, callback)
        }
    }

    private fun makeNameLetterCapital(name: String): String {
        var upperWord = ""
        val lineScan = Scanner(name)
        while (lineScan.hasNext()) {
            val word: String = lineScan.next()
            val otherWords = word.substring(1)
            upperWord += Character.toUpperCase(word[0]).toString() + otherWords.toLowerCase() + " "
        }
        return upperWord
    }

    private fun openProfile(message: LiveStreamMessage) {
        val userId =
            (message.user.extraData[ChatMessageData.USER_ID_KEY] as? Number)?.toInt()
                ?: 0
        callback.onUserTapped(userId)
    }

}

data class LiveStreamMessage(
    val user: User,
    val messageText: String,
    val isMine: Boolean,
    val streamMessage: Message? = null
)