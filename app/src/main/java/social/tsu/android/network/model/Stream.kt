package social.tsu.android.network.model

import com.squareup.moshi.Json

data class Stream(
    val sizes: List<StreamSize>,
    val duration: Long,
    val id: String,
    val orientation: String,
    @field:Json(name = "thumbnail_url")
    val thumbnail: String,
    val url: String
)
