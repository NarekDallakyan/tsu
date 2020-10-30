package social.tsu.android.network.model

import com.squareup.moshi.Json

data class BlockUserResponse(
    @field:Json(name = "message")
    val message: String
)