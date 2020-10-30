package social.tsu.android.ui.live_stream

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import com.getstream.sdk.chat.viewmodel.ChannelViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.controllers.ChannelController
import io.getstream.chat.android.client.models.Message
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.CommentDefaultInputAdapter
import social.tsu.android.adapters.CommentDefaultInputAdapterDelegate
import social.tsu.android.data.local.models.ChatMessageData
import social.tsu.android.data.local.models.ChatUserData
import social.tsu.android.databinding.LiveStreamMessageInputBinding
import social.tsu.android.ui.hideKeyboard
import social.tsu.android.utils.getSnackBar

private const val TAG = "MessageInputView"

class MessageInputView : ConstraintLayout, CommentDefaultInputAdapterDelegate {
    private lateinit var binding: LiveStreamMessageInputBinding

    private lateinit var channelController: ChannelController
    private val emojiAdaper: CommentDefaultInputAdapter by lazy {
        CommentDefaultInputAdapter(context.applicationContext as TsuApplication, this)
    }

    private lateinit var emojiFlightContainer: ViewGroup
    private lateinit var chatClient: ChatClient

    private val userProfilePictureUrl: String? by lazy {
        chatClient.getCurrentUser()?.extraData?.get(ChatUserData.AVATAR_URL_KEY) as? String
    }

    private val userId: Int by lazy {
        (chatClient.getCurrentUser()?.extraData?.get(ChatUserData.USER_ID_KEY) as? Number)?.toInt()
            ?: 0
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    fun init(context: Context) {
        val inflater = LayoutInflater.from(context)
        binding = LiveStreamMessageInputBinding.inflate(inflater, this, true)
    }

    fun setupEmojiList(emojiFlightContainer: ViewGroup) {
        this.emojiFlightContainer = emojiFlightContainer
        binding.chatEmojiList.adapter = emojiAdaper
    }

    fun setViewModel(
        viewModel: ChannelViewModel,
        lifecycleOwner: LifecycleOwner,
        channelController: ChannelController
    ) {
        chatClient = ChatClient.instance()
        this.channelController = channelController
        binding.lifecycleOwner = lifecycleOwner
        binding.viewModel = viewModel

        binding.chatSendMessage.setOnClickListener {
            Log.d(TAG, "chatSendMessage")
            val message = Message()
            message.text = binding.messageInput.text.toString()
            message.extraData = ChatMessageData(
                userId,
                false,
                userProfilePictureUrl
            ).getExtraData()
            viewModel.sendMessage(message).enqueue {
                if (it.isSuccess) {
                    viewModel.messageInputText.value = ""
                    context?.hideKeyboard(this)
                } else {
                    it.error().localizedMessage?.let { msg -> getSnackBar(msg) }
                    Log.e("LiveStreamChats", "Error sending message: ${it.error().message}")
                }
            }
        }

        // listen to typing events and connect to the view model
        binding.messageInput.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if (s.toString().isNotEmpty()) viewModel.keystroke()
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
            }
        })
    }

    override fun didTapOnEmoji(value: String) {
        val reactionMessage = Message().apply {
            text = value
            extraData[ChatMessageData.USER_ID_KEY] = userId
            extraData[ChatMessageData.IS_REACTION_KEY] = true
            userProfilePictureUrl?.let { extraData[ChatMessageData.PROFILE_PICTURE_URL_KEY] = it }
        }
        channelController.sendMessage(
            reactionMessage
        ).enqueue {
            if (it.isError) {
                Log.e("LiveStreamChats", "Error sending message: ${it.error().message}")
            }
        }
    }

}