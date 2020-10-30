package social.tsu.android.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.DefaultUserInfoService
import social.tsu.android.service.UserInfoService
import social.tsu.android.service.UserInfoServiceCallback

interface CreatePostMiniViewHolderCallback {
    fun didTapOnPost()
}

open class CreatePostMiniViewHolder(
    application: TsuApplication,
    private val callback: CreatePostMiniViewHolderCallback?,
    itemView: View
) : PostViewHolder(application, null, itemView), UserInfoServiceCallback {

    private val userService: UserInfoService by lazy {
        DefaultUserInfoService(application, this)
    }

    private val composeText = itemView.findViewById<ViewGroup>(R.id.composePost)

    fun bind() {
        composeText.setOnClickListener {
            callback?.didTapOnPost()
        }
        val cachedUserInfo = userService.getCachedUserInfo(AuthenticationHelper.currentUserId ?: 0)

        userProfileImageService.getProfilePicture(cachedUserInfo?.profilePictureUrl, false) {
            it?.let {
                userIcon?.setImageDrawable(it)
            }?: run {
                userIcon?.setImageResource(R.drawable.user)
            }
        }

    }

    override fun completedGetUserInfo(info: UserProfile?) {
    }

    override fun didErrorWith(message: String) {
    }
}
