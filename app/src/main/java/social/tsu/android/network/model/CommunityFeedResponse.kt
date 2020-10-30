package social.tsu.android.network.model

import com.squareup.moshi.Json
import social.tsu.android.data.local.entity.Post

data class CommunityFeedResponse(
    val posts: List<Post>,
    val meta: CommunityMetadata
)

data class CommunityMetadata(
    @field:Json(name = "next_page")
    val nexPage: Int?
)
