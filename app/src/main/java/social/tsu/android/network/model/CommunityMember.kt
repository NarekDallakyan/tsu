package social.tsu.android.network.model

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json


data class CommunityMember(
    val id: Int,
    val username: String,
    val fullname: String,
    @field:Json(name = "profile_picture_url")
    val profilePictureUrl: String?,
    @field:Json(name = "membership_id")
    val membershipId: Int,
    @field:Json(name = "trusted")
    val isTrusted: Boolean,
    val role: String,
    @field:Json(name = "owner")
    val isOwner: Boolean,
    val status: Int,
    @field:Json(name = "verified_status")
    var verifiedStatus: Int

) {
    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_MEMBER = "member"
    }

    val isAdmin: Boolean
        get() = role == ROLE_ADMIN
}

data class CommunityMembersResponse(
    val members: List<CommunityMember>
)

data class MemberKickRequest(
    @field:Json(name = "user_id")
    val userId: Int
)

data class CommunityMembersData(
    val owner: CommunityMember,
    val admins: List<CommunityMember>,
    val members: List<CommunityMember>
) {
    val size: Int
        get() {
            return 1 + admins.size + members.size
        }
}