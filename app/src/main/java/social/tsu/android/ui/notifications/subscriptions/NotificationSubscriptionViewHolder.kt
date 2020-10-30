package social.tsu.android.ui.notifications.subscriptions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.notification_subscription_item.view.*
import social.tsu.android.R
import social.tsu.android.data.local.entity.TsuSubscriptionTopic
import social.tsu.android.ui.util.BaseViewHolder

class NotificationSubscriptionViewHolder(
    itemView: View,
    private val actions: NotificationSubscriptionsAdapter.ViewHolderActions
): BaseViewHolder(itemView) {

    private val subscriptionSwitch = itemView.subscription_switch

    override fun <T> bind(item: T) {
        val subscription = item as TsuSubscriptionTopic

        subscriptionSwitch.text = subscription.description
        subscriptionSwitch.isChecked = subscription.subscribed

        subscriptionSwitch.setOnClickListener {
            actions.onSubscriptionStatusChanged(subscription,subscriptionSwitch.isChecked)
        }

    }


    companion object{
        fun create(parent: ViewGroup, actions: NotificationSubscriptionsAdapter.ViewHolderActions): NotificationSubscriptionViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.notification_subscription_item, parent, false)
            return NotificationSubscriptionViewHolder(
                view,
                actions
            )
        }
    }
}