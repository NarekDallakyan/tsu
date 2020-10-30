package social.tsu.android.network.model

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Location (

    val latitude : String?,
    @field:Json(name = "location_name")
    val locationName : String?,
    val longitude : String?
) : Parcelable
