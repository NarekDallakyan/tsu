package social.tsu.android.helper

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.navOptions
import social.tsu.android.R


fun NavController.showUserProfile(userId: Int?) {
    if (userId == null) return

    if (AuthenticationHelper.currentUserId == userId) {
        navigate(
            R.id.currentUserProfileFragment,
            bundleOf("id" to userId)
        )
    } else {
        navigate(
            R.id.showUserProfile,
            bundleOf("id" to userId)
        )
    }
}

fun NavController.showUserProfile(tag: String?) {
    if (tag == null) return

    if (AuthenticationHelper.currentUsername == tag) {
        navigate(
            R.id.currentUserProfileFragment,
            bundleOf("tag" to tag)
        )
    } else {
        navigate(
            R.id.showUserProfile,
            bundleOf("tag" to tag)
        )
    }
}