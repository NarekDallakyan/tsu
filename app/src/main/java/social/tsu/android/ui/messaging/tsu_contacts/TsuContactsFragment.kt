package social.tsu.android.ui.messaging.tsu_contacts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import social.tsu.android.R
import social.tsu.android.ui.community.BottomListener
import social.tsu.android.ui.messaging.tsu_contacts.followers.FollowersListFragment
import social.tsu.android.ui.messaging.tsu_contacts.friends.FriendsListFragment

class TsuContactsFragment : Fragment() {

    enum class ContactsMode {
        CHAT, COMMUNITY_INVITE
    }

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.contacts_nav_host_fragment) as NavHostFragment
    }
    private val contactsNavController by lazy {
        navHostFragment.navController
    }
    lateinit var viewPager: ViewPager
    var bottomListener: BottomListener?=null

    private val args by navArgs<TsuContactsFragmentArgs>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bottomListener = context as BottomListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tsu_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.view_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)

        addFragment()
        tabLayout.setupWithViewPager(viewPager)

        }

    override fun onStart() {
        super.onStart()
        bottomListener!!.messageClick()
    }


    override fun onResume() {
        super.onResume()
        addFragment()
    }

    private fun addFragment() {
        if (viewPager.adapter == null) {
            val adapter = TabViewPagerAdapter(childFragmentManager)
            adapter.addFragment(FriendsListFragment(), "Friends")
            adapter.addFragment(FollowersListFragment(), "followers")
            viewPager.adapter = adapter
        }
    }

    class TabViewPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        private val fragmentList = ArrayList<Fragment>()
        private val stringList = ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return fragmentList[position]
        }

        override fun getCount(): Int {
            return fragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return stringList[position]

        }

        fun addFragment(fragment: Fragment, title: String) {
            fragmentList.add(fragment)
            stringList.add(title)
        }
    }


}