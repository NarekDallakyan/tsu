package social.tsu.android.ui.community

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CommunityPageAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyCommunitiesFragment()
            1 -> CommunitySuggestedFragment()
            else -> CommunityCreateFragment(CommunityCreateFragmentArgs.fromBundle(Bundle()))
        }
    }

}

interface BottomListener {
    fun communityClick()
    fun messageClick()
}