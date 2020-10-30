package social.tsu.android.ui.post_feed.view_holders

import android.view.View
import android.view.ViewGroup
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.ImageFetchHandler
import social.tsu.android.service.UserInfoServiceCallback


open class CreatePostMiniViewHolder(
    application: TsuApplication,
    private val callback: ViewHolderActions,
    itemView: View
) : PostViewHolder(application,null, itemView), UserInfoServiceCallback {


    private val composeText = itemView.findViewById<ViewGroup>(R.id.composePost)


    override fun <T> bind(item: T) {
        composeText.setOnClickListener {
            callback.didTapPost()
        }
        val cachedUserInfo = callback.getCachedUserInfo(AuthenticationHelper.currentUserId ?: 0)

        callback.getProfilePicture(cachedUserInfo?.profilePictureUrl, false) {
            it?.let {
                userIcon?.setImageDrawable(it)
            } ?: run {
                userIcon?.setImageResource(R.drawable.user)
            }
        }

    }

    override fun completedGetUserInfo(info: UserProfile?) {
    }

    override fun didErrorWith(message: String) {
    }

    interface ViewHolderActions {
        fun getProfilePicture(
            key: String?,
            ignoringCache: Boolean,
            handler: ImageFetchHandler?
        )

        fun getCachedUserInfo(userId: Int): UserProfile?
        fun didTapPost()

    }
}
