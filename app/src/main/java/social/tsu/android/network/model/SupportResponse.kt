package social.tsu.android.network.model

import com.squareup.moshi.Json

data class SupportResponse(
    @field:Json(name = "data")
    val data: SupportData
)

data class SupportData(
    @field:Json(name = "message")
    val message: String
)