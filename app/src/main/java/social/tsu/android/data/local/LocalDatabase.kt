package social.tsu.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import social.tsu.android.data.local.dao.MessagingDao
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.dao.TsuNotificationDao
import social.tsu.android.data.local.entity.*
import social.tsu.android.network.model.Message

@Database(
    entities = [
        TsuNotification::class,
        TsuSubscriptionTopic::class,
        RecentContact::class,
        Post::class,
        FeedSourcePostJoin::class,
        FeedSource::class,
        Message::class,
        FeedOrder::class,
        FeedSourceOrderPostJoin::class
    ], version = 18, exportSchema = false
)
@TypeConverters(TsuTypeConverters::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun tsuNotificationDao():TsuNotificationDao
    abstract fun messagingDao(): MessagingDao
    abstract fun postFeedDao(): PostFeedDao


    companion object {
        private const val DATABASE_NAME = "Tsu Database"
        fun newInstance(context: Context) =
            Room.databaseBuilder(context, LocalDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}