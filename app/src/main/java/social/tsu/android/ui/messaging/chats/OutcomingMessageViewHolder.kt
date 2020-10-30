package social.tsu.android.ui.messaging.chats

import android.view.View
import social.tsu.android.network.model.Message

class OutcomingMessageViewHolder(
    itemView: View,
    callaback: MessageContentCallback

) : BaseMessageViewHolder(itemView, callaback) {

    override fun bind(message: Message, prevMessage: Message) {
        super.bind(message, prevMessage)
    }

}
