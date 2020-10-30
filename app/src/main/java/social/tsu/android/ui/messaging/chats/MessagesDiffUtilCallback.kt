package social.tsu.android.ui.messaging.chats

import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.network.model.Message


class MessagesDiffUtilCallback : DiffUtil.ItemCallback<Message>() {

    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        if (oldItem.id != newItem.id) return false
        if (oldItem.isRead != newItem.isRead) return false
        if (oldItem.createdAt != newItem.createdAt) return false
        if (oldItem.recipientId != newItem.recipientId) return false
        if (oldItem.senderId != newItem.senderId) return false
        if (oldItem.text != newItem.text) return false
        if (oldItem.pictureUrl != newItem.pictureUrl) return false
        if (oldItem.timestamp != newItem.timestamp) return false
        return true
    }

}