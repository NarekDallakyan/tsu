package social.tsu.android.data.local.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
data class PostUser(
    var id: Int? = null,

    var username: String? = null,

    @SerializedName("full_name")
    var fullName: String? = null,

    @SerializedName("profile_picture_url")
    var profilePictureUrl: String? = null,

    @SerializedName("verified_status")
    var verifiedStatus: Int? = null
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return if (other is PostUser) id == other.id else false
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }
}