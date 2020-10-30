package social.tsu.android.data.local.entity

import androidx.annotation.NonNull
import androidx.room.*
import com.squareup.moshi.Json
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.Comment
import social.tsu.android.network.model.Location
import social.tsu.android.network.model.Stream
import social.tsu.android.network.model.User
import social.tsu.android.utils.LinkExtractor
import java.util.*


data class PostPayload (val post: Post)
data class PostsPayload (val posts: List<Post>?)

data class OriginalPost (val id: Int)

@SuppressWarnings("ConstructorParameterNaming")
@Entity
data class Post(
    val is_share: Boolean = false,
    val has_liked: Boolean? = false,
    val has_shared: Boolean? = false,
    val popular: Boolean = false,
    val recipient_id: String?,
    val comments: List<Comment>,
    @PrimaryKey @NonNull val id: Long,
    val content: String,
    val title: String,
    val user_id: Int,
    val privacy: Int,
    val youtube_id: String?,
    val picture_width: Int?,
    val picture_height: Int?,
    val ad_id: String?,

    @field:Json(name = "group_id")
    val groupId: Int?,
    @field:Json(name = "group_friendly_id")
    val groupFriendlyId: String?,
    @field:Json(name = "group_name")
    val groupName: String?,

    val action: Int?,
    val has_link: Boolean = false,
    val has_stream: Boolean = false,
    val has_video: Boolean = false,
    val has_gif: Boolean = false,
    val has_picture: Boolean = false,
    val created_at: Date,
    val updated_at: String,
    val picture_url: String,
    val timestamp : Int,
    val provider : String,
    val location : Location,
    val user : User,
    val mentions : List<String>,
    val like_list : List<User?>,
    val share_list : List<User>,
    val curated : Boolean = false,
    val support_count : Int,
    val supporters_count : Int,
    val like_count : Int,
    val share_count : Int,
    val view_count : Int,
    val comment_count : Int,
    val pcurated : Int,
    val stream: Stream?,

    // share
    val shared_id: Int?,
    val original_user_id: Int?,
    val original_user: User?,
    val original_timestamp: Int?,
    val original_post: OriginalPost?

) {

    companion object {
        const val PRIVACY_PUBLIC = 0
        const val PRIVACY_PRIVATE = 1
        const val PRIVACY_EXCLUSIVE = 2
    }

    val isCurrentUserCreator: Boolean
        get() {
            val currentUserId = AuthenticationHelper.currentUserId
            return (user_id == currentUserId && !is_share) || original_user_id == currentUserId
        }

    val originalPostId: Int
        get() {
            if (is_share && shared_id != null) return shared_id
            return id.toInt()
        }

    val isSharable: Boolean
        get() {
            return privacy != PRIVACY_PRIVATE && has_shared != true
        }

    var preview: PostPreview? = null
    private var links: List<String>? = null

    fun getLinks(): List<String> {
        if (links == null) {
            links = LinkExtractor.extractLinks(content)
        }
        return links!!
    }

    fun setLinks(links: List<String>) {
        this.links = links
    }

    override fun hashCode(): Int {
        return id.toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        val post2 = other as Post
        return id == post2.id
    }

    fun contentEquals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Post

        if (is_share != other.is_share) return false
        if (has_liked != other.has_liked) return false
        if (has_shared != other.has_shared) return false
        if (popular != other.popular) return false
        if (recipient_id != other.recipient_id) return false
        if (comments != other.comments) return false
        if (id != other.id) return false
        if (content != other.content) return false
        if (title != other.title) return false
        if (user_id != other.user_id) return false
        if (privacy != other.privacy) return false
        if (youtube_id != other.youtube_id) return false
        if (picture_width != other.picture_width) return false
        if (picture_height != other.picture_height) return false
        if (ad_id != other.ad_id) return false
        if (groupId != other.groupId) return false
        if (action != other.action) return false
        if (has_link != other.has_link) return false
        if (has_stream != other.has_stream) return false
        if (has_video != other.has_video) return false
        if (has_gif != other.has_gif) return false
        if (has_picture != other.has_picture) return false
        if (created_at != other.created_at) return false
        if (updated_at != other.updated_at) return false
        if (picture_url != other.picture_url) return false
        if (timestamp != other.timestamp) return false
        if (provider != other.provider) return false
        if (location != other.location) return false
        if (user != other.user) return false
        if (mentions != other.mentions) return false
        if (like_list != other.like_list) return false
        if (share_list != other.share_list) return false
        if (curated != other.curated) return false
        if (like_count != other.like_count) return false
        if (share_count != other.share_count) return false
        if (view_count != other.view_count) return false
        if (comment_count != other.comment_count) return false
        if (pcurated != other.pcurated) return false
        if (stream != other.stream) return false
        if (shared_id != other.shared_id) return false
        if (original_user_id != other.original_user_id) return false
        if (original_user != other.original_user) return false
        if (original_timestamp != other.original_timestamp) return false
        if (original_post != other.original_post) return false
        if (links != other.links) return false

        return true
    }


}

@Entity(tableName = "feed_source")
data class FeedSource(
    @PrimaryKey @NonNull val type: Type
) {
    enum class Type {
        MAIN, COMMUNITY, USER, USER_PHOTOS, USER_VIDEOS, DISCOVERY_FEED, HASHTAG, ORDER
    }
}

@Entity(
    tableName = "feedsource_posts_join",
    primaryKeys = ["source_type", "post_id"],
    foreignKeys = [
        ForeignKey(
            entity = FeedSource::class,
            parentColumns = ["type"],
            childColumns = ["source_type"]
        ),
        ForeignKey(
            entity = Post::class,
            parentColumns = ["id"],
            childColumns = ["post_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FeedSourcePostJoin(
    @ColumnInfo(name = "source_type")
    val sourceType: FeedSource.Type,
    @ColumnInfo(name = "post_id")
    val postId: Long
)

@Entity(tableName = "feed_order", indices = [Index(value = ["post_id"], unique = true)])
data class FeedOrder(
    @PrimaryKey(autoGenerate = true)
    val order: Int = 0,
    @ColumnInfo(name = "post_id")
    val postId: Long
)

@Entity(
    tableName = "feed_order_posts_join",
    primaryKeys = ["order_id", "post_id"],
    indices = [Index(value = ["order_id"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = FeedSource::class,
            parentColumns = ["type"],
            childColumns = ["source_type"]
        ),
        ForeignKey(
            entity = FeedOrder::class,
            parentColumns = ["order"],
            childColumns = ["order_id"]
        ),
        ForeignKey(
            entity = Post::class,
            parentColumns = ["id"],
            childColumns = ["post_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FeedSourceOrderPostJoin(
    @ColumnInfo(name = "source_type")
    val sourceType: FeedSource.Type,
    @ColumnInfo(name = "post_id")
    val postId: Long,
    @ColumnInfo(name = "order_id")
    val orderId: Int = 0
)

data class PostPreview(
    val link: String,
    val host: String?,
    val title: String?,
    val description: String?,
    val imageUrl: String?
)
