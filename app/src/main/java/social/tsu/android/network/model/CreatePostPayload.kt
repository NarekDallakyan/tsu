package social.tsu.android.network.model

import com.squareup.moshi.Json

data class CreatePostPayload(
    val content: String,
    @field:Json(name = "stream_id")
    val streamId: String? = null,
    val privacy: Int = 0
)
