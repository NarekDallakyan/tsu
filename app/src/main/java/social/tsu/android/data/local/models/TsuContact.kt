package social.tsu.android.data.local.models

import com.google.gson.annotations.SerializedName


class TsuContact {
    var id: Int = 0

    var username: String?= null

    @SerializedName("profile_picture_url")
    var profilePictureUrl: String?= null

    @SerializedName("cover_picture_url")
    var coverPictureUrl: String?= null

    @SerializedName("follower_count")
    var followerCount: Int = 0

    @SerializedName("following_count")
    var followingCount: Int = 0

    var gender: String?= null

    @SerializedName("verified_status")
    var verifiedStatus: Int?= null

    var role: String?= null

    @SerializedName("full_name")
    var fullName: String?= null

    @SerializedName("friend_count")
    var friendCount: Int?= null

    @SerializedName("is_friend")
    var isFriend: Boolean?= null

    @SerializedName("friendship_status")
    var friendshipStatus: String?= null

    @SerializedName("is_following")
    var isFollowing: Boolean?= null

    @SerializedName("is_wall_private")
    var isWallPrivate: Boolean?= null

    @SerializedName("accept_friend_request")
    var acceptFriendRequest: Boolean?= null

    override fun equals(other: Any?): Boolean {
        return if (other is TsuContact) other.id == id else false
    }

    enum class Type{
        FRIEND, FOLLOWER, FOLLOWING
    }

    fun toPostUser(): PostUser {
        return PostUser(id,username, fullName, profilePictureUrl, verifiedStatus)
    }

}