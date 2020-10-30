package social.tsu.android.network.model

import com.squareup.moshi.Json

data class CommunityPendings (
    val total: Int,
    @field:Json(name = "users")
    val pendingRequests: List<PendingRequest>
)

data class PendingRequest (
    val id: Int,
    val username: String,
    val fullname: String,
    @field:Json(name = "profile_picture_url") val profilePictureUrl: String,
    @field:Json(name = "membership_id") val membershipId: Int
)