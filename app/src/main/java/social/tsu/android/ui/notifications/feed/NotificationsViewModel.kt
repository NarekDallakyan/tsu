package social.tsu.android.ui.notifications.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.data.repository.TsuNotificationRepository
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class NotificationsViewModel @Inject constructor(private val notificationRepo: TsuNotificationRepository) : ViewModel(){

    val loadState by lazy {
        notificationRepo.loadState
    }

    val myNotifications by lazy {
        notificationRepo.notifications
    }

    val unseenNotificationsCount by lazy {
        notificationRepo.unseenNotificationsCount
    }

    fun markSeen(){
        notificationRepo.markAsSeen()
    }

    fun markAsRead(notification: TsuNotification){
        notificationRepo.markAsRead(notification)
    }

    fun acceptFriend(tsuNotification: TsuNotification): LiveData<Data<Boolean>> {
        return notificationRepo.acceptFriendRequest(tsuNotification)
    }

    fun declineFriend(tsuNotification: TsuNotification): LiveData<Data<Boolean>> {
        return notificationRepo.declineFriendRequest(tsuNotification)
    }

    fun retryNotificationFetch() {
        notificationRepo.retryNotificationFetch()
    }

    fun refreshNotifications() {
        notificationRepo.refreshNotifications()
    }

}