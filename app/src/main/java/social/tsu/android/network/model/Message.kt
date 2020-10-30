package social.tsu.android.network.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import social.tsu.android.helper.AuthenticationHelper


@Entity(tableName = "ChatMessages")
data class Message(
    @PrimaryKey
    @NonNull
    val id: Int,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("recipient_id") val recipientId: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("conversation_id") val conversationId: Int,
    val text: String,
    @SerializedName("picture_url") val pictureUrl: String?,
    val timestamp: Int
) {
    val otherUser: Int
        get() {
            if (senderId == AuthenticationHelper.currentUserId) {
                return recipientId
            }
            return senderId
        }


    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        val otherMessage = other as Message
        return id == otherMessage.id
    }

}

data class CreateMessageDTO(
    val message: CreateMessagePayload
)

data class CreateMessagePayload(
    @SerializedName("sender_id") val senderId: Int = 0,
    @SerializedName("recipient_id") val recipientId: Int = 0,
    val body: String = "",
    val picture: String = ""
)

data class MarkAsReadDTO(
    @SerializedName("message_ids") val messageIds: IntArray
)