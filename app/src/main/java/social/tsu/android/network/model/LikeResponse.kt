package social.tsu.android.network.model

import com.squareup.moshi.Json

data class LikeResponse (
    @field:Json(name = "post_id")
    val postId: Int,
    @field:Json(name = "like_count")
    val likeCount: Int,
    @field:Json(name = "message")
    val mesaage: String,
    @field:Json(name = "button_text")
    val buttonText: String,
    @field:Json(name = "like_list")
    val likeList: List<String>
)