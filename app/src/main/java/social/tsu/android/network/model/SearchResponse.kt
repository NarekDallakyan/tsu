package social.tsu.android.network.model

import com.squareup.moshi.Json

/*
{
  "data": {
    "users": [
      {
        "id": 36,
        "username": "badluckbob",
        "profile_picture_url": "/assets/user-72e8e09f4fcf823137916c638a54e813900d72471c8824478739a71ccc5a2e2c.png",
        "verified_status": 0,
        "url": "/badluckbob",
        "full_name": "Bob Badluck"
      }
    ]
  }
}

 */
data class DataWrapper<T>(val data: T, val error: Boolean = false, val message: String? = null)

data class SearchResponse(
    val user: List<SearchUser> = listOf(),
    val hashtag: List<HashTag> = listOf(),
    val group: List<Group> = listOf()
)

data class HashTag(
    val id: Int,
    val text: String?,
    @field:Json(name = "main_hashtag_id")
    val mainHashTagId: Int
)


data class SearchUsersResponse(
    val users: List<SearchUser> = listOf()
)

data class SearchUser(
    @field:Json(name = "full_name") val fullName: String,
    val id: Int,
    val username: String,
    @field:Json(name = "profile_picture_url") val profilePictureUrl: String,
    @field:Json(name = "verified_status") val verifiedStatus: Int,
    val url: String
)

data class MentionUsersResponse(@field:Json(name = "data") val users: List<MentionUser> = listOf())

data class MentionUser(
    @field:Json(name = "full_name") val fullName: String,
    val id: Int,
    val username: String,
    @field:Json(name = "profile_picture_url") val profilePictureUrl: String,
    @field:Json(name = "verified_status") val verifiedStatus: Int,
    val url: String
)
