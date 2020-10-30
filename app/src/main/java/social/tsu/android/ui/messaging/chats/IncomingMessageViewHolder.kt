package social.tsu.android.ui.messaging.chats

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import social.tsu.android.R
import social.tsu.android.network.model.Message


class IncomingMessageViewHolder(
    itemView: View,
    callaback: MessageContentCallback
) : BaseMessageViewHolder(itemView, callaback) {

    private val profileImage: ImageView? = itemView.findViewById(R.id.chat_message_profile_image)

    fun bind(message: Message, prevMessage: Message, otherUserImage: Drawable?) {
        super.bind(message, prevMessage)

        if (otherUserImage != null) {
            profileImage?.setImageDrawable(otherUserImage)
        } else {
            profileImage?.setImageResource(R.drawable.user)
        }
    }
}