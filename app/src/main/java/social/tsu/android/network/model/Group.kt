package social.tsu.android.network.model

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Group(
    val id: Int,
    val name: String,
    val description: String,
    val visibility: String,
    @field:Json(name = "members_count")
    var membersCount: Int,
    @field:Json(name = "requests_count")
    val requestsCount: Int,
    val language: String,
    val published: Boolean,
    val slug: String?,
    @field:Json(name = "picture_url")
    val pictureUrl: String,
    val layout: String,
    @field:Json(name = "owner_id")
    val ownerId: Int,
    @field:Json(name = "owner_name")
    val ownerName: String?,
    @field:Json(name = "require_moderation")
    val requireModeration: Boolean,
    @field:Json(name = "parent_id")
    val parentId: Int,
    val ordering: String,
    val timestamp: Long,
    @field:Json(name = "unseen_posts")
    val unseenPosts: Int?,
    @field:Json(name = "parent_name")
    val parentName: String?,
    val membership: Membership? = null
) : Parcelable {
    companion object{
        /**
         * Special group object to act as a placeholder where you need to pass dummy group
         */
        val PlaceholderGroup = Group(
            -1, "","","",-1, -1, "",
            false, "","","",-1, "", false, -1,
            "",-1, -1, ""
        )

        fun createMockGroupForEdit(id: Int, name: String): Group {
            return Group(
                id, name,"","",-1, -1, "",
                false, "","","",-1, null, false, -1,
                "",-1, -1, ""
            )
        }
    }
}