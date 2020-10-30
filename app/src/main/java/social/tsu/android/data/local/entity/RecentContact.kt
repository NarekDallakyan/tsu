package social.tsu.android.data.local.entity

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import social.tsu.android.data.local.models.PostUser
import social.tsu.android.helper.AuthenticationHelper

@Entity
data class RecentContact(
    
    @PrimaryKey
    @NonNull
    var id: Int = 0,

    @SerializedName("is_read")
    var isRead: Boolean? = null,

    @SerializedName("created_at")
    var createdAt: String,

    @SerializedName("recipient_id")
    var recipientId: Int? = null,

    @SerializedName("sender_id")
    var senderId: Int?= null,

    @SerializedName("conversation_id")
    var conversationId: Int? = null,

    var text: String?= null,

    @SerializedName("picture_url")
    var pictureUrl: String? = null,

    var sender: PostUser? = null,

    var recipient: PostUser? = null
) {

    val otherUser: PostUser?
        get() = if (sender != null && sender?.id != AuthenticationHelper.currentUserId) {
            sender
        } else recipient

}