package social.tsu.android.network.model


import com.squareup.moshi.Json

data class User(
    val firstname: String? = null,
    val lastname: String? = null,
    val email: String? = null,
    val username: String? = null,
    @field:Json(name = "accept_tos") val acceptTos: String,
    val password: String? = null,
    @field:Json(name = "profile_picture_url") val profilePictureUrl: String? = null,
    @field:Json(name = "verified_status") val verifiedStatus: Int? = null,
    @field:Json(name = "full_name") val fullName: String? = null,
    @field:Json(name = "invited_by_id") val invitedById: Int? = null,
    @field:Json(name = "hubspot_id") val hubspotId: Int? = null,
    @field:Json(name = "invited_by_username") val invitedByUsername: String? = null,
    val id: Long? = 0
)


data class LoginRequest(
    val login: String,
    val password: String,
    @field:Json(name = "device_id")
    val deviceId: String,
    @field:Json(name = "client_version")
    val clientVersion: String
)

data class CreateAccountRequest(
    val user: User,
    @field:Json(name = "device_id") val deviceId: String,
    @field:Json(name = "birth_day") val birthDay: String,
    @field:Json(name = "birth_month") val birthMonth: String,
    @field:Json(name = "birth_year") val birthYear: String,
    @field:Json(name = "accept_tos") val acceptTos: String,
    @field:Json(name = "old_email") val oldEmail: String? = null
)

data class CreateAccountResponsePayload(
    @field:Json(name = "accept_friend_request") var accceptFriendRequest: Boolean?,
    @field:Json(name = "auth_token") var authToken: String?,
    @field:Json(name = "birthday") val birthday: String?,
    @field:Json(name = "cover_picture_url") val coverPhoto: String?,
    @field:Json(name = "email") val email: String?,
    @field:Json(name = "follower_count") val followerCount: Int?,
    @field:Json(name = "following_count") val followingCount: Int?,
    @field:Json(name = "friend_count") val friendCount: Int?,
    @field:Json(name = "friendship_status") val friendStatus: String?,
    @field:Json(name = "full_name") val fullName: String?,
    val gender: String?,
    val id: Int?,
    @field:Json(name = "is_birthday_private") val bdayPrivate: Boolean?,
    @field:Json(name = "is_following") val isfollowing: Boolean?,
    @field:Json(name = "is_friend") val isfriend: Boolean?,
    @field:Json(name = "phone_number") val phone: String?,
    @field:Json(name = "profile_picture_url") val profilePic: String?,
    @field:Json(name = "role") val role: String?,
    @field:Json(name = "username") val username: String?,
    @field:Json(name = "verified_status") val verified: Int?
)

data class CreateAccountResponse(val data: CreateAccountResponsePayload?,
                                 val error: Boolean = false, val message: String?)

/**
 * 		"invited_by_username":"runbai",
"username":"meetme1",
"email":"meetme1@gmail.com",
"password":"123456abc",
"firstname":"Joseph",
"lastname":"Tester",
"gender":"Male",
"accept_tos": "true"
 *
 * 	"device_id": "hellophone",
"birth_day": "23",
"birth_year": "1989",
"birth_month": "09",
"accept_tos": "true"
 */
