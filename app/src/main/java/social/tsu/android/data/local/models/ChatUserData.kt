package social.tsu.android.data.local.models

data class ChatUserData(
    val userId: Int,
    val username: String,
    val name: String,
    val avatarUrl: String? = null,
    val verified: Int?
) {
    fun getExtraData(): MutableMap<String, Any> {
        return mutableMapOf<String, Any>(
            USER_ID_KEY to userId,
            NAME_KEY to name
        ).apply {
            avatarUrl?.let { put(AVATAR_URL_KEY, it) }
            verified?.let { put(VERIFIED_STATUS, verified) }
        }
    }

    companion object {
        const val USER_ID_KEY = "userId"
        const val NAME_KEY = "name"
        const val AVATAR_URL_KEY = "avatarURL"
        const val VERIFIED_STATUS = "verifyStatus"
    }
}

enum class ChatUserType(val key: String) {
    SHERIFF("sheriff"),
    ADMIN("admin"),
    USER("user")
}

data class ChatMessageData(
    val userId: Int,
    val isReaction: Boolean,
    val profilePictureUrl: String?
) {
    fun getExtraData(): MutableMap<String, Any> {
        return mutableMapOf<String, Any>(
            USER_ID_KEY to userId,
            IS_REACTION_KEY to isReaction
        ).apply {
            profilePictureUrl?.let { put(PROFILE_PICTURE_URL_KEY, it) }
        }
    }

    companion object {
        const val USER_ID_KEY = "userId"
        const val IS_REACTION_KEY = "isReaction"
        const val PROFILE_PICTURE_URL_KEY = "profilePictureUrl"
    }
}