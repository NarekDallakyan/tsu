package social.tsu.android.network.model

import com.squareup.moshi.Json
import social.tsu.android.data.local.entity.Post

data class AnalyticsResponse(
    @field:Json(name = "view_count")
    val viewCount: Int,
    @field:Json(name = "like_count")
    val likeCount: Int,
    @field:Json(name = "comment_count")
    val commentCount: Int,
    @field:Json(name = "share_count")
    val shareCount: Int = 0,
    val views: List<GraphItemResponse>,
    val likes: List<GraphItemResponse>,
    val comments: List<GraphItemResponse>,
    val shares: List<GraphItemResponse>,
    val posts: List<Post>
)