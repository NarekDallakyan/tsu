package social.tsu.android.ui.notifications.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import social.tsu.android.R
import social.tsu.android.data.local.entity.TsuNotification

class GeneralNotificationViewHolder(
    itemView: View,
    private val callback: NotificationAdapter.ViewHolderActions
) : BaseNotificationViewHolder(callback, itemView) {


    override fun <T> bind(item: T) {
        val notification = item as TsuNotification

        super.bind(notification)

        itemView.setOnClickListener { callback.onNotificationClick(notification) }

    }

    companion object {
        fun create(
            parent: ViewGroup,
            actions: NotificationAdapter.ViewHolderActions
        ): GeneralNotificationViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.notification_item, parent, false)
            return GeneralNotificationViewHolder(
                view,
                actions
            )
        }
    }
}