package social.tsu.android.network.model

import com.squareup.moshi.Json
import social.tsu.android.data.local.models.OldUserDetails

data class VerifyInviteRequest(val email: String)

class VerifyInviteResponse {
    @field:Json(name = "email_in_use")
    var emailInUse: Boolean? = null

    @field:Json(name = "old_tsu_user")
    var oldTsuUser: OldUserDetails? = null

    @field:Json(name = "hubspot_id")
    var hubspotId: Int? = null

    @field:Json(name = "invited_by_username")
    var invitedByUsername: String? = null
}

data class VerifyOldTsuUserRequest(
    val email: String,
    @field:Json(name = "verification_code") val verificationCode: Int
)
