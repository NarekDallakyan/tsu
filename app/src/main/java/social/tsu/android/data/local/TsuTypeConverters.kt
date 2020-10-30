package social.tsu.android.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import social.tsu.android.data.local.entity.*
import social.tsu.android.data.local.models.PostUser
import social.tsu.android.data.local.models.TsuNotificationType
import social.tsu.android.network.model.Comment
import social.tsu.android.network.model.Location
import social.tsu.android.network.model.Stream
import social.tsu.android.network.model.User
import java.util.*


class TsuTypeConverters {

    private val gson = Gson()

    @TypeConverter
    fun toString(obj: Any?): String? {
        return gson.toJson(obj)
    }

    @TypeConverter
    fun toTsuNotificationType(string: String): TsuNotificationType? {
        return gson.fromJson(string, TsuNotificationType::class.java)
    }

    @TypeConverter
    fun toTsuNotificationResource(string: String): TsuNotificationResource? {
        return gson.fromJson(string, TsuNotificationResource::class.java)
    }

    @TypeConverter
    fun toExtra(string: String): Extra? {
        return gson.fromJson(string, Extra::class.java)
    }

    @TypeConverter
    fun toActionUser(string: String): ActionUser? {
        return gson.fromJson(string, ActionUser::class.java)
    }

    @TypeConverter
    fun toTsuNotificationCategory(string: String): TsuNotificationCategory? {
        return gson.fromJson(string, TsuNotificationCategory::class.java)
    }

    @TypeConverter
    fun toPostUser(string: String): PostUser? {
        return gson.fromJson(string, PostUser::class.java)
    }

    @TypeConverter
    fun toPostPreview(string: String): PostPreview? {
        return gson.fromJson(string, PostPreview::class.java)
    }

    @TypeConverter
    fun toStringList(string: String): List<String>? {
        return gson.fromJson(string, object : TypeToken<List<String>>() {}.type)
    }

    @TypeConverter
    fun toCommentList(string: String): List<Comment>? {
        return gson.fromJson(string, object : TypeToken<List<Comment>>() {}.type)
    }

    @TypeConverter
    fun toDate(string: String): Date? {
        return gson.fromJson(string, Date::class.java)
    }

    @TypeConverter
    fun toLocation(string: String): Location? {
        return gson.fromJson(string, Location::class.java)
    }

    @TypeConverter
    fun toUser(string: String): User? {
        return gson.fromJson(string, User::class.java)
    }

    @TypeConverter
    fun toUserList(string: String): List<User> {
        return gson.fromJson(string, object : TypeToken<List<User>>() {}.type) ?: listOf()
    }

    @TypeConverter
    fun toStream(string: String): Stream? {
        return gson.fromJson(string, Stream::class.java)
    }

    @TypeConverter
    fun toOriginalPost(string: String): OriginalPost? {
        return gson.fromJson(string, OriginalPost::class.java)
    }

    @TypeConverter
    fun toFeedSource(string: String): FeedSource.Type {
        return gson.fromJson(string, FeedSource.Type::class.java)
    }

}