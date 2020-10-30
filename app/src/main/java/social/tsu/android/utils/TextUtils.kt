package social.tsu.android.utils

import android.content.Context
import social.tsu.android.R
import social.tsu.android.data.local.entity.Post


object TSURegex {
    const val HASHTAG = "#([A-Za-z0-9_-]+)"
    const val USERNAME = "@([\\.A-Za-z0-9_-]+)"
}

private const val MAX_UNSHORTENED_NOTIFICATIONS_COUNT = 9

fun shortenCountForNotificationBabge(ctx: Context, num: Int): String {
    return if (num in 0..MAX_UNSHORTENED_NOTIFICATIONS_COUNT) num.toString() else ctx.getString(
        R.string.notification_badge_overflow,
        MAX_UNSHORTENED_NOTIFICATIONS_COUNT
    )
}

fun privacystring (privacy: Int):String{
    if(privacy == Post.PRIVACY_EXCLUSIVE){
        return "exclusive"
    } else if (privacy == Post.PRIVACY_PRIVATE) {
        return "private"
    }
    return "public"
}

fun extractUsername(value: String?): String? {

    val prefix = "@"
    return when {
        value == null -> {
            return null
        }
        value.startsWith(prefix) -> {
            value.substringAfter(prefix)
        }
        else -> {
            value
        }
    }
}

fun sanitizeUserName(username: String) : String {
    return username.replace(".", "_")
}

fun String.hashtags(): List<String> {
    return Regex(TSURegex.HASHTAG).findAll(this).map { it.value }.toList()
}