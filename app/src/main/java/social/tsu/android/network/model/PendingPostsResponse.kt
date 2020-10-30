package social.tsu.android.network.model

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

data class PendingPostsResponse(
    @field:Json(name = "pending_group_posts")
    val posts: List<PendingPost>
)

@Parcelize
data class PendingPost(
    val id: Int,
    val content: String,
    @field:Json(name = "picture_url")
    val pictureUrl: String,
    @field:Json(name = "show_on_profile")
    val showOnProfile: Boolean,
    val title: String,
    val user: PendingPostUser?= null,
    @field:Json(name = "created_at")
    val createdAt: Int,
    var communityId: Int? = null
) : Parcelable

@Parcelize
data class PendingPostUser(
    val id: Int,
    val fullname: String,
    @field:Json(name = "profile_picture_url")
    val profilePictureUrl: String?= null
) : Parcelable

/*
{
    "pending_group_posts": [
        {
            "id": 31,
            "content": "Hi",
            "picture_url": "",
            "show_on_profile": false,
            "title": "",
            "user": {
                "id": 575,
                "fullname": "gtPwcToqPtxT YjrJuvrFIjsAUBW",
                "profile_picture_url": "/assets/user.png"
           },
            "created_at": 1591610443
        }
    ]
}*/
