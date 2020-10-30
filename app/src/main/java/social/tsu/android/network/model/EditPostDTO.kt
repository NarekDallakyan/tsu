package social.tsu.android.network.model

data class EditPostDTO(
    val content: String,
    val id: Long
)

data class EditPostResponse(
    val content: String,
    val mentions: List<String>
)