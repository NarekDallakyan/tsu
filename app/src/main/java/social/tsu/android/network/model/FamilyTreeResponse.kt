package social.tsu.android.network.model

import com.squareup.moshi.Json

data class FamilyTreeResponse(
    val children: List<UserProfile>,
    @field:Json(name = "invited_by")
    val invitedBy: UserProfile?,
    val user: UserProfile,
    @field:Json(name = "children_count")
    val childrenCount: Int
)