package social.tsu.android.ui.notifications.subscriptions


import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.entity.TsuSubscriptionTopic
import social.tsu.android.data.repository.TsuNotificationRepository
import javax.inject.Inject

class NotificationSubscriptionViewModel @Inject constructor(private val notificationRepo: TsuNotificationRepository) : ViewModel(){

    val loadState by lazy {
        notificationRepo.loadState
    }

    val myNotificationSubscriptions by lazy {
        notificationRepo.notificationSubscriptions
    }


    fun setNewSubscriptionStatus(tsuSubscriptionTopic: TsuSubscriptionTopic, newStatus:Boolean) {

        notificationRepo.updateSubscriptionStatus(tsuSubscriptionTopic, newStatus)
    }

}