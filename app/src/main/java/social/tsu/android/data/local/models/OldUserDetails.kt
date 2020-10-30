package social.tsu.android.data.local.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OldUserDetails(
    val id: Int = 0,
    val old_id: Int = 0,
    val firstname: String? = null,
    val lastname: String? = null,
    val username: String? = null,
    val email: String? = null,
    val phone_number: String? = null,
    val birthdate: String? = null,
    val bio: String? = null,
    val website: String? = null,
    val youtube: String? = null,
    val pintrest: String? = null,
    val tumblr: String? = null,
    val user_id: String? = null,
    val username_in_use: Boolean? = null
) : Parcelable

