package social.tsu.android.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import social.tsu.android.R
import social.tsu.android.network.model.Group


class CommunityFragment : Fragment(), CommunityCreateListener {

    lateinit var viewPager: ViewPager2
    private val model: CommunityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val view = inflater.inflate(R.layout.fragment_community, container, false)

        viewPager = view.findViewById(R.id.view_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)

        val communityPageAdapter = CommunityPageAdapter(this)
        viewPager.adapter = communityPageAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.communities_tab_my_communities)
                1 -> tab.text = getString(R.string.communities_tab_suggested)
                2 -> tab.text = getString(R.string.communities_tab_create)
            }
        }.attach()

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.communityListeners.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        model.communityListeners.remove(this)
    }

    override fun onCommunityCreated() {
        viewPager.setCurrentItem(0, true)
    }

    override fun onCommunityChanged(group: Group) {
    }

    override fun onCommunityDeleted() {
    }

}