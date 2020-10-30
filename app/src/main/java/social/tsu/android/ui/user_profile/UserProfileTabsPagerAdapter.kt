package social.tsu.android.ui.user_profile

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import social.tsu.android.ui.MessageFragment
import social.tsu.android.ui.MessageFragment.Companion.KEY_MESSAGE
import social.tsu.android.ui.post_feed.user_feed.UserFeedFragment

class UserProfileTabsPagerAdapter(
    context: Context,
    private val userId: Int,
    fm: FragmentManager,
    val userProfileFragment: UserProfileFragment,
    private val dividerColor: Int
) : FragmentStateAdapter(userProfileFragment) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putLong("userId", userId.toLong())
        bundle.putInt("dividerColor", dividerColor)

        return when (position) {
            /*0 -> {
                val userAboutFragment = UserAboutFragment()
                userAboutFragment.arguments = bundle
                userAboutFragment
            }*/
            0 -> {
                val userFeedFragment = UserFeedFragment()
                userFeedFragment.arguments = bundle
                userFeedFragment
            }
            1 -> {
                val userPhotosFragment = UserPhotosFragment()
                userPhotosFragment.arguments = bundle
                userPhotosFragment
            }
            2 -> {
                val userVideosFragment = UserVideosFragment()
                userVideosFragment.arguments = bundle
                userVideosFragment
            }
            else -> {
                val messageFragment = MessageFragment()
                messageFragment.arguments =
                    bundleOf(KEY_MESSAGE to "Placeholder for number $position fragment")
                messageFragment
            }
        }
    }
}