package social.tsu.android.network.model

import com.squareup.moshi.Json

data class Comment(
    val id: Int,
    val body: String,
    val userId: Int,
    @field:Json(name = "like_count") var likeCount: Int,
    @field:Json(name = "reply_count") val replyCount: Int,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    val timestamp: Int,
    val mentions: List<String>?,
    @field:Json(name = "post_id") val postId: Int,
    @field:Json(name = "parent_id") val parentId: Int,
    val user: User,
    @field:Json(name = "has_liked") var hasLiked: Boolean,
    @field:Json(name = "like_list") val likeList: List<CommentUser>?
)

data class CommentUser(
    val id: Int,
    val username: String,
    @field:Json(name = "full_name") val fullName: String,
    @field:Json(name = "profile_picture_url") val profilePictureUrl: String,
    @field:Json(name = "verified_status")val verifiedStatus: Int
)