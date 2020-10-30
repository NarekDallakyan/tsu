package social.tsu.android.network.model

import com.squareup.moshi.Json

data class ProfileEditInfoDTO(
    @field:Json(name = "user")
    var user: ProfileEditDTO
)

data class ProfileEditDTO (
    @field:Json(name = "profile_picture")
    val profilePicture: String?,
    @field:Json(name = "cover_picture")
    val coverPicture: String?,
    @field:Json(name = "bio")
    val bio: String?,
    @field:Json(name = "youtube")
    val youtube: String?,
    @field:Json(name = "website")
    val website: String?,
    @field:Json(name = "twitter")
    val twitter: String?,
    @field:Json(name = "instagram")
    val instagram: String?,
    @field:Json(name = "facebook")
    val facebook: String?,
    @field:Json(name = "relationship_status")
    val relationship_status: Int?,
    @field:Json(name = "relationship_with_id")
    val relationship_with_id: Int?,
    @field:Json(name = "full_name")
    val full_name: String?,
    @field:Json(name = "birthday")
    val birthday: String?,
    @field:Json(name = "hometown")
    val hometown: String?,
    @field:Json(name = "name_pronunciation")
    val name_pronunciation: String?
) {
    class Builder {
        private var profilePicture: String? = null
        private var coverPicture: String? = null
        private var bio: String? = null
        private var youtube: String? = null
        private var website: String? = null
        private var twitter: String? = null
        private var instagram: String? = null
        private var facebook: String? = null
        private var relationship_status: Int? = null
        private var relationship_with_id: Int? = null
        private var fullname: String? = null
        private var birthday: String? = null
        private var hometown: String? = null
        private var name_pronunciation: String? = null

        fun profilePicture(profilePicture: String?) = apply { this.profilePicture = profilePicture }
        fun coverPicture(coverPicture: String?) = apply { this.coverPicture = coverPicture }
        fun bio(bio: String?) = apply { this.bio = bio }
        fun youtube(youtube: String?) = apply { this.youtube = youtube}
        fun website(website: String?) = apply { this.website = website }
        fun fullname(fullname: String?) = apply { this.fullname = fullname }
        fun twitter(twitter: String?) = apply { this.twitter = twitter }
        fun instagram(instagram: String?) = apply { this.instagram = instagram }
        fun facebook(facebook: String?) = apply { this.facebook = facebook }
        fun birthday(birthday: String?) = apply { this.birthday = birthday }
        fun hometown(hometown: String?) = apply { this.hometown = hometown }
        fun namePronunciation(name_pronunciation: String?) = apply { this.name_pronunciation = name_pronunciation }
        fun relationshipStatus(relationship_status: Int?) = apply { this.relationship_status = relationship_status }
        fun relationshipWithId(relationship_with_id: Int?) = apply { this.relationship_with_id = relationship_with_id }

        fun build() = ProfileEditDTO(profilePicture, coverPicture, bio, youtube, website, twitter, instagram, facebook, relationship_status, relationship_with_id, fullname, birthday, hometown, name_pronunciation)
    }
}