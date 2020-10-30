package social.tsu.android.network.model

import com.squareup.moshi.Json

data class BlockUserPayload (
    @field:Json(name = "user_id")
    val userId: Int)

