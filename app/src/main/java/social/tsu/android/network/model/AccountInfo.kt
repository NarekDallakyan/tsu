package social.tsu.android.network.model

import com.squareup.moshi.Json


data class AccountInfoDTO(
    val user: AccountInfoUser
)

data class AccountInfoUser(
    @field:Json(name = "id")
    val id: Int,
    @field:Json(name = "firstname")
    val firstName: String,
    @field:Json(name = "lastname")
    val lastName: String,
    @field:Json(name = "email")
    val email: String,
    @field:Json(name = "username")
    val username: String,
    @field:Json(name = "current_password")
    val currentPassword: String,
    @field:Json(name = "phone_number")
    val phoneNumber: String?
)