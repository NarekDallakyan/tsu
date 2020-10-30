package social.tsu.android.network.model

import com.squareup.moshi.Json

data class CommunityPayload(
    val group: Community
)

data class CommunityResponse(
    val group: Group,
    val message: String?
)

data class CommunityListResponse(
    val groups: List<Group>
)

data class Community(
    val name: String,
    val description: String,
    @field:Json(name = "parent_id")
    val topicId: Int,
    @field:Json(name = "require_moderation")
    val moderation: Boolean,
    val visibility: String,
    val picture: String?,
    val ordering: String = "popular"
)