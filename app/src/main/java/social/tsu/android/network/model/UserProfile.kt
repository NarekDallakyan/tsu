package social.tsu.android.network.model

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize
import social.tsu.android.data.local.models.PostUser

/*
  "id": 4,
        "username": "precious",
        "full_name": "Loyd Breitenberg",
        "profile_picture_url": "/assets/user.png",
        "cover_picture_url": "/cover_pictures/original/missing.png",
        "friend_count": 0,
        "follower_count": 3,
        "following_count": 4,
        "gender": "Female",
        "birthday": "",
        "verified_status": 1,
        "role": "",
        "is_birthday_private": false,
        "firstname": "Loyd",
        "lastname": "Breitenberg",
        "website": "",
        "bio": "",
        "youtube": "",
        "pinterest": "",
        "tumblr": "",
        "hometown": null,
        "current_location": null,
        "relationship_status": "Married",
        "relationship_with_id": "3",
        "relationship_with": "Mary Breitenberg",
        "has_donation_button": false,
        "is_message_private": false,
        "is_wall_private": true,
        "accept_friend_request": true,
        "created_at_int": 1579031958,
        "is_user_trusted": false,
        "is_tsu_public": true,
        "is_post_content_private": false,
        "phone_number": "",
        "default_channel_id": "",
        "is_friend": false,
        "friendship_status": null,
        "is_following": false,
        "is_blocked": false,
        "has_private_membership": false
    }
 */
data class UserProfile(
    @field:Json(name = "id") // 4,
    val id: Int,
    @field:Json(name = "username") // "precious",
    val username: String,
    @field:Json(name = "email")
    val email: String,
    @field:Json(name = "full_name") // "Loyd Breitenberg",
    val fullName: String,
    @field:Json(name = "profile_picture_url") // "/assets/user.png",
    val profilePictureUrl: String,
    @field:Json(name = "cover_picture_url") // "/cover_pictures/original/missing.png",
    val coverPictureUrl: String,
    @field:Json(name = "friend_count") // 0,
    val friendCount: Int,
    @field:Json(name = "follower_count") // 3,
    val followerCount: Int,
    @field:Json(name = "following_count") // 4,
    val followingCount: Int,
    @field:Json(name = "gender") // "Female",
    val gender: String,
    @field:Json(name = "birthday") // "",
    val birthday: String?,
    @field:Json(name = "verified_status") // 1,
    val verifiedStatus: Int,
    @field:Json(name = "role") // "",
    val role: String?,
    @field:Json(name = "is_birthday_private") // false,
    val isBirthdayPrivate: Boolean,
    @field:Json(name = "firstname") // "Loyd",
    val firstname: String,
    @field:Json(name = "lastname") // "Breitenberg",
    val lastname: String,
    @field:Json(name = "website") // "",
    val website: String,
    @field:Json(name = "bio") // "",
    val bio: String? = "",
    @field:Json(name = "youtube") // "",
    val youtube: String,
    @field:Json(name = "facebook") // "",
    val facebook: String?,
    @field:Json(name = "twitter") // "",
    val twitter: String?,
    @field:Json(name = "instagram") // "",
    val instagram: String?,
    @field:Json(name = "pinterest") // "",
    val pinterest: String,
    @field:Json(name = "tumblr") // "",
    val tumblr: String,
    @field:Json(name = "hometown") // null,
    val hometown: String?,
    @field:Json(name = "current_location") // null,
    val currentLocation: Location?,
    @field:Json(name = "relationship_status") // null,
    val relationshipStatus: String?,
    @field:Json(name = "relationship_with_id")
    val relationshipWithId: Int?,
    @field:Json(name = "relationship_with")
    val relationshipWith: String?,
    @field:Json(name = "has_donation_button") // false,
    val hasDonationButton: Boolean,
    @field:Json(name = "is_message_private") // false,
    val isMessagePrivate: Boolean,
    @field:Json(name = "is_wall_private") // true,
    val isWallPrivate: Boolean,
    @field:Json(name = "accept_friend_request") // true,
    val acceptFriendRequest: Boolean,
    @field:Json(name = "created_at_int") // 1579031958,
    val createdAtInt: Int?,
    @field:Json(name = "is_user_trusted") // false,
    val isUserTrusted: Boolean,
    @field:Json(name = "is_tsu_public") // true,
    val isTsuPublic: Boolean,
    @field:Json(name = "is_post_content_private") // false,
    val isPostContentPrivat: Boolean,
    @field:Json(name = "phone_number") // "",
    val phoneNumber: String?,
    @field:Json(name = "default_channel_id") // "",
    val defaultChannelId: String,
    @field:Json(name = "is_friend") // false,
    val isFriend: Boolean,
    @field:Json(name = "friendship_status") // null,
    val friendshipStatus: String?,
    @field:Json(name = "is_following") // false,
    val isFollowing: Boolean,
    @field:Json(name = "is_blocked") // false,
    val isBlocked: Boolean,
    @field:Json(name = "has_private_membership") // false
    val hasPrivateMembership: Boolean,
    @field:Json(name = "can_message") // false
    val canMessage: Any?,
    @field:Json(name = "name_pronunciation") //empty
    val namePronunciation: String?,
    @field:Json(name = "is_friends_private")
    val isFriendsPrivate: Boolean?,
    @field:Json(name = "is_following_private")
    val isFollowingPrivate: Boolean?,
    @field:Json(name = "is_followers_private")
    val isFollowersPrivate: Boolean?,
    @field:Json(name = "badges")
    val badges: List<Badges>?


) {
    var userFriendshipStatus: UserFriendshipStatus = UserFriendshipStatus.UNKNOWN
        get() {
            val status = friendshipStatus ?: return UserFriendshipStatus.UNKNOWN
            when (status) {
                "accepted" -> return UserFriendshipStatus.ACCEPTED
                "pending" -> return UserFriendshipStatus.PENDING
                "requested" -> return UserFriendshipStatus.REQUESTED
            }
            return UserFriendshipStatus.UNKNOWN
        }

    fun toPostUser(): PostUser {
        return PostUser(id, username, fullName, profilePictureUrl, verifiedStatus)
    }

    fun toProfileParams(): UserProfileParams {
        return UserProfileParams(
            id,
            username,
            email,
            fullName,
            profilePictureUrl,
            coverPictureUrl,
            friendCount,
            followerCount,
            followingCount,
            gender,
            birthday,
            verifiedStatus,
            role,
            isBirthdayPrivate,
            firstname,
            lastname,
            website,
            bio,
            youtube,
            facebook,
            twitter,
            instagram,
            pinterest,
            tumblr,
            hometown,
            currentLocation,
            relationshipStatus,
            relationshipWithId,
            relationshipWith,
            hasDonationButton,
            isMessagePrivate,
            isWallPrivate,
            acceptFriendRequest,
            createdAtInt,
            isUserTrusted,
            isTsuPublic,
            isPostContentPrivat,
            phoneNumber,
            defaultChannelId,
            isFriend,
            friendshipStatus,
            isFollowing,
            isBlocked,
            hasPrivateMembership,
            namePronunciation
        )
    }

    override fun toString(): String {
        return "id: $id \n name: $username \n isShow: $canMessage"
    }

    companion object {
        const val NO_USER_ID = -1
    }
}

@Parcelize
data class UserProfileParams(
    val id: Int?,
    val username: String?,
    val email: String?,
    val fullName: String?,
    val profilePictureUrl: String?,
    val coverPictureUrl: String?,
    val friendCount: Int?,
    val followerCount: Int?,
    val followingCount: Int?,
    val gender: String?,
    val birthday: String?,
    val verifiedStatus: Int?,
    val role: String?,
    val isBirthdayPrivate: Boolean?,
    val firstname: String?,
    val lastname: String?,
    val website: String?,
    val bio: String? = "",
    val youtube: String?,
    val facebook: String?,
    val twitter: String?,
    val instagram: String?,
    val pinterest: String?,
    val tumblr: String?,
    val hometown: String?,
    val currentLocation: Location?,
    val relationshipStatus: String?,
    val relationshipWithId: Int?,
    val relationshipWith: String?,
    val hasDonationButton: Boolean?,
    val isMessagePrivate: Boolean?,
    val isWallPrivate: Boolean?,
    val acceptFriendRequest: Boolean?,
    val createdAtInt: Int?,
    val isUserTrusted: Boolean?,
    val isTsuPublic: Boolean?,
    val isPostContentPrivat: Boolean?,
    val phoneNumber: String?,
    val defaultChannelId: String?,
    val isFriend: Boolean?,
    val friendshipStatus: String?,
    val isFollowing: Boolean?,
    val isBlocked: Boolean?,
    val hasPrivateMembership: Boolean?,
    val namePronunciation: String?

) : Parcelable {
    var userFriendshipStatus: UserFriendshipStatus = UserFriendshipStatus.UNKNOWN
        get() {
            val status = friendshipStatus ?: return UserFriendshipStatus.UNKNOWN
            when (status) {
                "accepted" -> return UserFriendshipStatus.ACCEPTED
                "pending" -> return UserFriendshipStatus.PENDING
                "requested" -> return UserFriendshipStatus.REQUESTED
            }
            return UserFriendshipStatus.UNKNOWN
        }
}

enum class UserFriendshipStatus {
    UNKNOWN,
    PENDING,
    ACCEPTED,
    REQUESTED
}

enum class BadgesStatus {
    GOLD,
    PLATINUM,
    DIAMOND
}

@Parcelize
data class Badges(
    @field:Json(name = "id")
    val id: Int?,
    @field:Json(name = "title")
    val title: String?
) : Parcelable