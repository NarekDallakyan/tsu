package social.tsu.android.ui.messaging.chats

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.Message


private const val VIEW_MESSAGE_OUTCOMING = 0
private const val VIEW_MESSAGE_INCOMING = 1
private const val VIEW_IMAGE_MESSAGE_OUTCOMING = 2
private const val VIEW_IMAGE_MESSAGE_INCOMING = 3

class ChatMessagesAdapter(
    private val callback: Callback
) : PagedListAdapter<Message, BaseMessageViewHolder>(MessagesDiffUtilCallback()) {

    interface Callback : MessageContentCallback {
        fun markAsRead(message: Message)
    }

    var otherUserDrawable: Drawable? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_MESSAGE_INCOMING, VIEW_IMAGE_MESSAGE_INCOMING -> {
                val view = inflater.inflate(R.layout.item_message_incoming, parent, false)
                IncomingMessageViewHolder(view, callback)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_message_outcoming, parent, false)
                OutcomingMessageViewHolder(view, callback)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item?.senderId == AuthenticationHelper.currentUserId) {
            if (item?.pictureUrl != null) VIEW_IMAGE_MESSAGE_OUTCOMING else VIEW_MESSAGE_OUTCOMING
        } else {
            if (item?.pictureUrl != null) VIEW_IMAGE_MESSAGE_INCOMING else VIEW_MESSAGE_INCOMING
        }
    }

    override fun onBindViewHolder(holder: BaseMessageViewHolder, position: Int) {
        val message = getItem(position) ?: return
        val nextPosition = if (position+1 != currentList?.size ?: 0) position +1 else position
        val nextMessage = getItem(nextPosition) ?: return

        if (holder is IncomingMessageViewHolder) {
            holder.bind(message, nextMessage, otherUserDrawable)
        } else {
            holder.bind(message, nextMessage)
        }

        if (message.senderId != AuthenticationHelper.currentUserId && !message.isRead) {
            callback.markAsRead(message)
        }
    }

}

