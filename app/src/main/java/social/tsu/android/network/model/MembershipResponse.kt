package social.tsu.android.network.model

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

data class MembershipResponse(
    val memberships : List<Membership>
)

@Parcelize
data class Membership (
    val id: Int,
    var status: String,
    var role: String,
    val group: Group,
    @field:Json(name = "invited_by_id")
    val invitedById: Int?,
    @field:Json(name = "add_content")
    val addContent: Boolean
) : Parcelable {

    enum class Status {
        ACCEPTED, PENDING, PENDING_PROMOTION;

        fun getString(): String {
            return when (this) {
                ACCEPTED -> "accepted"
                PENDING -> "pending"
                PENDING_PROMOTION -> "pending_promotion"
            }
        }
    }

    fun getRole(userId: Int) : Role {
        return when {
            //nope, group can be null for some reason on some devices
            //so adding null check for non-null variable...
            group != null && group.ownerId == userId -> Role.OWNER
            role == "admin" -> Role.ADMIN
            role == "member" -> Role.MEMBER
            else -> Role.NONE
        }
    }

    fun getStatus(): Status? {
        return when (status) {
            "accepted" -> Status.ACCEPTED
            "pending" -> Status.PENDING
            "pending_promotion" -> Status.PENDING_PROMOTION
            else -> null
        }
    }
}

enum class Role {
    OWNER, ADMIN, MEMBER, NONE;

    fun getString(): String {
        return when (this) {
            OWNER -> "owner"
            ADMIN -> "admin"
            MEMBER -> "member"
            NONE -> "none"
        }
    }
}
