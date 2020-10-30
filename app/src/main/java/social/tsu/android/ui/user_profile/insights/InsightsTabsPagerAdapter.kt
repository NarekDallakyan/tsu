package social.tsu.android.ui.user_profile.insights

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import social.tsu.android.ui.user_profile.insights.analytics.UserAnalyticsFragment
import social.tsu.android.ui.user_profile.insights.family_tree.UserFamilyTreeFragment

class InsightsTabsPagerAdapter(insightsFragment: InsightsFragment) :
    FragmentStateAdapter(insightsFragment) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserAnalyticsFragment.newInstance()
            1 -> UserFamilyTreeFragment.newInstance()
            else -> UserAnalyticsFragment.newInstance()
        }
    }

}