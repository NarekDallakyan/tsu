package social.tsu.android.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import org.threeten.bp.Instant
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.data.local.entity.TsuSubscriptionTopic
import social.tsu.android.data.local.models.TsuNotificationSubscriptions
import social.tsu.android.data.local.models.TsuNotificationType

@Dao
interface TsuNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTsuNotifications(vararg tsuNotification: TsuNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTsuSubscriptionTopics(vararg tsuSubscriptionTopic: TsuSubscriptionTopic)

    @Query("SELECT * FROM TsuSubscriptionTopic ORDER BY category ASC, description ASC")
    fun getAllSubscriptions():List<TsuSubscriptionTopic>

    @Query("SELECT * FROM TsuNotification ORDER BY timestamp DESC")
    fun getTsuNotifications():DataSource.Factory<Int, TsuNotification>

    @Query("SELECT * FROM TsuSubscriptionTopic ORDER BY category ASC, description ASC")
    fun getSubscriptions():DataSource.Factory<Int, TsuSubscriptionTopic>

    @Transaction
    fun saveTsuSubscriptionTopics(result: TsuNotificationSubscriptions){
        val topics = result.subscriptions.map { it.topics.map { topic-> topic.apply {
            category = it.category
        } } }
        var anySubscribed = false
        topics.forEach {
            saveTsuSubscriptionTopics(*it.toTypedArray())
            if(!anySubscribed && it.any { topic -> topic.subscribed }) {
                anySubscribed= true
            }
        }
        val allSubscriptionTopic = TsuSubscriptionTopic().apply {
            description = "All Notifications"
            category = "All Notification"
            name = "all_notifications"
            subscribed = anySubscribed
        }
        saveTsuSubscriptionTopics(allSubscriptionTopic)

    }

    @Query("SELECT * FROM tsunotification WHERE seenAt IS NULL")
    fun getUnseenNotifications():List<TsuNotification>

    @Query("SELECT * FROM tsunotification ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentNotification():TsuNotification?

    @Query("SELECT COUNT(*) FROM tsunotification WHERE seenAt IS NULL")
    fun getUnseenNotificationsCount():LiveData<Int>

    @Query("SELECT COUNT(*) FROM tsunotification")
    fun getNotificationsCount():Int

    @Query("UPDATE tsunotification SET seenAt =:currentTimeStamp WHERE dbId=:dbId")
    fun markAsSeen(dbId: String, currentTimeStamp:Long = Instant.now().epochSecond)

    @Query("UPDATE tsunotification SET readAt =:currentTimeStamp WHERE dbId=:dbId")
    fun markAsRead(dbId: String, currentTimeStamp:Long = Instant.now().epochSecond)

    @Delete
    fun deleteNotification(tsuNotification: TsuNotification)

    @Query("DELETE FROM tsunotification WHERE actionUserId=:actionUserId AND notificationType=:notificationType")
    fun deleteNotification(actionUserId: Int, notificationType: TsuNotificationType)

    @Delete
    fun deleteNotification(tsuSubscriptionTopic: TsuSubscriptionTopic)

}