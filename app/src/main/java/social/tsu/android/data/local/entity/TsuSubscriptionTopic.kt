package social.tsu.android.data.local.entity

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class TsuSubscriptionTopic {
    lateinit var category: String
    lateinit var description: String
    @PrimaryKey @NonNull lateinit var name: String
    var subscribed: Boolean = false
}