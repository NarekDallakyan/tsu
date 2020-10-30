package social.tsu.android.network.model

import com.google.gson.annotations.SerializedName
import social.tsu.android.data.local.entity.TsuNotification


data class NotificationFeedResponse(

    @SerializedName("unread_count")
    var unreadCount: Int? = null,
    @SerializedName("unseen_count")
    var unseenCount: Int? = null,
    var timestamp: Long? = null,
    var notifications: List<TsuNotification>? = null,
    @SerializedName("count_limit")
    var countLimit: Int? = null,
    var cursor: String? = null

)


data class NotificationSummaryResponse(

    @SerializedName("unread_count")
    var unreadCount: Int? = null,
    @SerializedName("unseen_count")
    var unseenCount: Int? = null,
    var timestamp: Long? = null,
    var summary: NotificationSummary? = null,
    @SerializedName("count_limit")
    var countLimit: Int? = null,
    var cursor: String? = null

)


data class NotificationSummary(

    var general: List<TsuNotification>? = null,
    @SerializedName("friend_requests")
    var friendRequests: List<TsuNotification>? = null,
    var messages: List<TsuNotification>? = null

)