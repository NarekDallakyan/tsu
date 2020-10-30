package social.tsu.android.data.local.entity

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import social.tsu.android.data.local.models.TsuNotificationType

@Entity
class TsuNotification {

    @PrimaryKey
    @NonNull
    var dbId: String = ""
        get() = "$category $timestamp $message"

    var id: Int = 0
    var category: String? = null
    var categoryType: TsuNotificationCategory = TsuNotificationCategory.UNKNOWN
        get() = TsuNotificationCategory.values().firstOrNull { it.key == category }
            ?: TsuNotificationCategory.UNKNOWN

    var type: String? = null
    var notificationType: TsuNotificationType = TsuNotificationType.UNKNOWN
        get() = TsuNotificationType.values().firstOrNull { it.key == type }
            ?: TsuNotificationType.UNKNOWN

    var timestamp: Long? = null

    @SerializedName("action_user_id")
    var actionUserId: Long? = null

    @SerializedName("read_at")
    var readAt: Long? = null

    @SerializedName("seen_at")
    var seenAt: Long? = null
    var message: String? = null
    @SerializedName("marked_message")
    var markedMessage: String? = null

    @SerializedName("picture_url")
    var pictureUrl: String? = null
    var resource: TsuNotificationResource? = null

    @SerializedName("extra")
    var extra: Extra? = null
}

class TsuNotificationResource {
    var type: String = ""
    var resourceType: ResourceType = ResourceType.UNKNOWN
        get() = ResourceType.values().firstOrNull { it.key == type } ?: ResourceType.UNKNOWN

    var id: String? = null
    var parameters: TsuCustomParameters? = null
    var preview: String? = null
}

class TsuCustomParameters {

    @SerializedName("full_name")
    var fullName: String? = null

    @SerializedName("user_id")
    var userId: Int? = null

    @SerializedName("username")
    var username: String? = null
}

class Extra {
    @SerializedName("action_user")
    var actionUser: ActionUser? = null
}

class ActionUser {
    @SerializedName("verified_status")
    var verifiedStatus: Int? = null
}

enum class TsuNotificationCategory(val key: String) {
    UNKNOWN(""),
    FRIEND_REQUESTS("friend_requests"),
    GENERAL("general"),
    MESSAGES("messages")
}

enum class ResourceType(val key: String) {
    UNKNOWN(""),
    USER("user"),
    GROUP("group"),
    POST("post"),
    MESSAGES("messages")
}