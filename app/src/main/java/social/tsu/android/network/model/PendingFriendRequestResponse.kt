package social.tsu.android.network.model

import com.squareup.moshi.Json

/*
{
    "data": {
        "has_more": false,
        "cursor": 1581111103,
        "pending": {
            "total": 1,
            "friend_requests": [
                {
                    "user_id": 214,
                    "username": "toosilly",
                    "full_name": "Todd Sillyman",
                    "profile_url": "http://matt-fargate-lb-1020943446.us-east-1.elb.amazonaws.com/toosilly",
                    "profile_picture_url": "/assets/user.png",
                    "created_at": 1581111103
                }
            ]
        }
    }
}
 */
//data class DataWrapper<T>(val data: T)

data class FriendRequestResponse(
    val has_more: Boolean,
    val cursor: Long?,
    val pending: Pendings
)

data class Pendings (
    val total: Int,
    @field:Json(name = "friend_requests")
    val friendRequests: List<FriendRequest>
)

data class FriendRequest (
    val user_id: Int,
    val username: String,
    val full_name: String, //: "Todd Sillyman",
    @field:Json(name = "profile_picture_url") val profilePictureUrl: String,
    @field:Json(name = "profile_url") val profileUrl: String,
    @field:Json(name = "created_at") val createdAt: Long // 1581111103
)