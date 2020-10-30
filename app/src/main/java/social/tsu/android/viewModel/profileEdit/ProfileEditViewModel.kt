package social.tsu.android.viewModel.profileEdit

import android.net.Uri
import social.tsu.android.TsuApplication
import social.tsu.android.network.model.ProfileEditDTO
import social.tsu.android.network.model.ProfileEditInfoDTO
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.*
import social.tsu.android.ui.util.ImageUtils.Companion.imageDataFrom

interface ProfileEditViewModelCallback: DefaultServiceCallback {
    fun loadProfile(profile: UserProfile)
    fun completedProfileUpdate(profile: UserProfile)
}

abstract class ProfileEditViewModel: SettingsServiceCallback, UserInfoServiceCallback {

    abstract var dtoBuilder: ProfileEditDTO.Builder
    abstract var currentRelationshipUser: UserProfile?

    abstract fun updateProfilePicture(uri: Uri?)
    abstract fun updateCoverPicture(uri: Uri?)
    abstract fun updateBio(value: String)
    abstract fun updateWebsite(value: String)
    abstract fun updateYoutube(value: String)
    abstract fun updateTwitter(value: String)
    abstract fun updateInstagram(value: String)
    abstract fun updateFacebook(value: String)
    abstract fun updateRelationshipStatus(value: Int)
    abstract fun updateRelationshipWith(value: Int)

    abstract fun updateProfile()
    abstract fun getUserInfo(userId: Int)
}

class DefaultProfileEditViewModel(
    val application: TsuApplication,
    val callback: ProfileEditViewModelCallback
): ProfileEditViewModel() {

    override var dtoBuilder: ProfileEditDTO.Builder = ProfileEditDTO.Builder()

    override var currentRelationshipUser: UserProfile? = null
        get() = userService.getCachedUserInfo(currentUserInfo?.relationshipWithId ?: -1)

    private var currentUserInfo: UserProfile? = null

    private val service: DefaultSettingsService by lazy {
        DefaultSettingsService(application, this)
    }

    private val userService: DefaultUserInfoService by lazy {
        DefaultUserInfoService(application, this)
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        currentUserInfo = info
        info?.let {
            callback.loadProfile(info)
        } ?: run {
            callback.didErrorWith("Failed to get user info")
        }
    }

    override fun completedUserProfileUpdate(info: UserProfile) {
        callback.completedProfileUpdate(info)
    }

    override fun failedToUpdateUserProfile(message: String?) {
        callback.didErrorWith(message?: "failedToUpdateUserProfile")
    }

    override fun didErrorWith(message: String) {
        callback.didErrorWith(message)
    }

    override fun updateBio(value: String) {
        dtoBuilder.bio(value)
    }

    override fun updateInstagram(value: String) {
        dtoBuilder.instagram(value)
    }

    override fun updateTwitter(value: String) {
        dtoBuilder.twitter(value)
    }

    override fun updateWebsite(value: String) {
        dtoBuilder.website(value)
    }

    override fun updateYoutube(value: String) {
        dtoBuilder.youtube(value)
    }

    override fun updateFacebook(value: String) {
        dtoBuilder.facebook(value)
    }

    override fun updateRelationshipStatus(value: Int) {
        dtoBuilder.relationshipStatus(value)
    }

    override fun updateRelationshipWith(value: Int) {
        dtoBuilder.relationshipWithId(value)
    }

    override fun updateCoverPicture(uri: Uri?) {
        imageDataFrom(uri)?.let {
            dtoBuilder.coverPicture(it)
        }
    }

    override fun getUserInfo(userId: Int) {
        userService.getUserInfo(userId, false)
    }

    override fun updateProfilePicture(uri: Uri?) {
        imageDataFrom(uri)?.let {
            dtoBuilder.profilePicture(it)
        }
    }

    override fun updateProfile() {
        val dto = ProfileEditInfoDTO(dtoBuilder.build())
        service.updateInfo(dto)
    }
}