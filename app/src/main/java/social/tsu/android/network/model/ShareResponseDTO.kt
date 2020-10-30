package social.tsu.android.network.model

import com.squareup.moshi.Json

data class ShareResponseDTO(
    @field:Json(name = "post_id")
    val postId: Long,
    @field:Json(name = "share_count")
    val shareCount: Int,
    @field:Json(name = "share_list")
    val shareList: List<String>,
    @field:Json(name = "message")
    val message: String
)