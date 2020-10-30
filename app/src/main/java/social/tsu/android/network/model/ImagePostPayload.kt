package social.tsu.android.network.model

data class ImagePostPayload(
    val content: String,
    val picture: String?,
    val gif: String?,
    val privacy: Int = 0
)


