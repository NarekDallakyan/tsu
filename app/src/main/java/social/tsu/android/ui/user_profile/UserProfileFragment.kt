package social.tsu.android.ui.user_profile

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.user_profile.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.Constants
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.model.BadgesStatus
import social.tsu.android.network.model.UserFriendshipStatus
import social.tsu.android.network.model.UserProfile
import social.tsu.android.network.model.UserProfileParams
import social.tsu.android.service.UserProfileImageService
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.post.view.PostTypesFragmentDirections
import social.tsu.android.ui.setVisibleOrGone
import social.tsu.android.ui.user_profile.friends.UserFriendListType
import social.tsu.android.utils.*
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.set


class UserProfileFragment : Fragment() {

    private var currentFollowStatus: Boolean = false
    private lateinit var userInfoForAbout: UserProfileParams

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    val viewModel by viewModels<UserProfileViewModel> { viewModelFactory }

    private var userId: Int = UserProfile.NO_USER_ID

    @Inject
    lateinit var userProfileImageService: UserProfileImageService

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    val args: UserProfileFragmentArgs by navArgs()

    private var appBar: AppBarLayout? = null
    lateinit var toolbar: Toolbar
    lateinit var toolbarTitle: TextView

    private val numberFormat = NumberFormat.getInstance().also {
        it.maximumFractionDigits = 1
    }

    private var viewPager: ViewPager2? = null

    private var coverPhoto: ImageView? = null
    private var userProfilePhoto: ImageView? = null
    private var followBtn: MaterialButton? = null
    private var friendBtn: MaterialButton? = null
    private var messageBtn: MaterialButton? = null
    private var bankBtn: MaterialButton? = null
    private var supportBtn: MaterialButton? = null
    private var addPostBtn: MaterialButton? = null
    private var actionBtn: FloatingActionButton? = null
    private var friendsCountText: TextView? = null
    private var followersCountText: TextView? = null
    private var followingCountText: TextView? = null
    private var profileHeader: ConstraintLayout? = null
    private var currentFriendStatus = UserFriendshipStatus.UNKNOWN
    private var currentTab: Int = 0
    private var viewMore: View? = null
    private var ownButtonsContainer: View? = null
    private var otherButtonsContainer: View? = null
    private var statusIcon: ImageView? = null
    private var tabLayout: TabLayout? = null
    private var rootLayout: CoordinatorLayout? = null
    val properties = HashMap<String, Any?>()

    private val isCurrentUserProfile: Boolean
        get() = (userId == AuthenticationHelper.currentUserId || args.tag == AuthenticationHelper.currentUsername)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        val view = inflater.inflate(R.layout.user_profile, container, false)
        viewPager = view?.findViewById(R.id.sectionsViewPager)
        coverPhoto = view?.findViewById(R.id.user_profile_backgroundView)
        userProfilePhoto = view?.findViewById(R.id.user_profile_avatarView)
        followBtn = view?.findViewById(R.id.user_profile_follow_btn)
        friendBtn = view?.findViewById(R.id.user_profile_friend_btn)
        messageBtn = view?.findViewById(R.id.user_profile_message_btn)
        bankBtn = view?.findViewById(R.id.user_profile_bank_btn)
        supportBtn = view?.findViewById(R.id.user_profile_support_me_btn)
        addPostBtn = view?.findViewById(R.id.user_profile_add_post_btn)
        actionBtn = view?.findViewById(R.id.user_profile_action_button)
        friendsCountText = view?.findViewById(R.id.friendCountText)
        followersCountText = view?.findViewById(R.id.followerCountText)
        followingCountText = view?.findViewById(R.id.followingCountText)
        profileHeader = view.findViewById(R.id.user_profile_header)
        toolbarTitle = view.findViewById(R.id.toolbar_title)
        appBar = view.findViewById(R.id.appbar)
        toolbar = view.findViewById(R.id.toolbar)
        viewMore = view.findViewById(R.id.user_info_row_more)
        ownButtonsContainer = view.findViewById(R.id.user_profile_own_buttons)
        otherButtonsContainer = view.findViewById(R.id.user_profile_other_buttons)
        statusIcon = view.findViewById(R.id.user_profile_status_icon)
        tabLayout = view.findViewById(R.id.tabLayout)
        rootLayout = view.findViewById(R.id.user_motion_container)

        viewModel.userProfileLiveData.observe(viewLifecycleOwner, Observer {
            userId = it.id

            callingMyEditProfile()

        })

        viewModel.isBlock.observe(viewLifecycleOwner, Observer {
            if (it) {
                coverPhoto?.setImageResource(R.drawable.ic_no_cover_small)
                userProfilePhoto?.setImageResource(R.drawable.user)
                actionBtn?.hide()
                user_info_group?.hide()
                sections_divider?.hide()
                tabLayout?.hide()
                viewPager?.hide()
                user_block_message?.show()
                profileHeader?.updateLayoutParams<AppBarLayout.LayoutParams> {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                }
            } else {
                actionBtn?.show()
                user_info_group?.show()
                sections_divider?.show()
                tabLayout?.show()
                viewPager?.show()
                user_block_message?.hide()
                profileHeader?.updateLayoutParams<AppBarLayout.LayoutParams> {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                }
            }
        })

        if (viewModel.userId != UserProfile.NO_USER_ID) {
            userId = viewModel.userId
        }

        if (!args.tag.isNullOrEmpty() && !args.tag.isNullOrBlank()) {
            viewModel.loadTagUser("" + args.tag)
        } else {
            if (args.id == -1) {
                AuthenticationHelper.currentUserId?.let {
                    viewModel.reloadUserInfo(it)
                    userId = it
                } ?: run {
                    findNavController().navigate(R.id.showLoginFragment, null, NavOptions.Builder().setLaunchSingleTop(true).build())
                }
            } else {
                userId = args.id
                viewModel.reloadUserInfo(args.id)
            }
        }

        view?.findViewById<View>(R.id.user_info_friends_container)?.setOnClickListener {
            val firstName = viewModel.firstName ?: return@setOnClickListener

            val label = if (isCurrentUserProfile) {
                getString(R.string.user_friends_list_title_my)
            } else {
                getString(R.string.user_friends_list_title_other, firstName)
            }
            findNavController().navigate(
                R.id.userFriendsFragment,
                bundleOf(
                    "id" to userId,
                    "list_type" to UserFriendListType.FRIEND_LIST.ordinal,
                    "toolbar_title" to label
                )
            )
        }

        view?.findViewById<View>(R.id.user_info_followers_container)?.setOnClickListener {
            val firstName = viewModel.firstName ?: return@setOnClickListener

            val label = if (isCurrentUserProfile) {
                getString(R.string.user_followers_list_title_my)
            } else {
                getString(R.string.user_followers_list_title_other, firstName)
            }
            findNavController().navigate(
                R.id.userFriendsFragment,
                bundleOf(
                    "id" to userId,
                    "list_type" to UserFriendListType.FOLLOWER_LIST.ordinal,
                    "toolbar_title" to label
                )
            )
        }

        view?.findViewById<View>(R.id.user_info_following_container)?.setOnClickListener {
            val firstName = viewModel.firstName ?: return@setOnClickListener

            val label = if (isCurrentUserProfile) {
                getString(R.string.user_followings_list_title_my)
            } else {
                getString(R.string.user_followings_list_title_other, firstName)
            }
            findNavController().navigate(
                R.id.userFriendsFragment,
                bundleOf(
                    "id" to userId,
                    "list_type" to UserFriendListType.FOLLOWING_LIST.ordinal,
                    "toolbar_title" to label
                )
            )
        }

        if (isCurrentUserProfile) {
            (activity as MainActivity).setCustomToolbarWithDrawer(toolbar)
        } else {
            (activity as MainActivity).setCustomToolbar(toolbar)
        }

        user_profile_status_icon.hide()
        viewModel.userProfileLiveData.observe(viewLifecycleOwner, Observer(this::updateUserInfo))
        viewModel.currentFriendStatus.observe(
            viewLifecycleOwner,
            Observer(this::updateFriendStatus)
        )
        viewModel.isFollowingLiveData.observe(
            viewLifecycleOwner,
            Observer(this::updateFollowStatus)
        )
        viewModel.messageLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) snack(it)
        })

        callingMyEditProfile()
        return view
    }

    private fun callingMyEditProfile() {
        if (isCurrentUserProfile) {
            initActionButton(true)
            initOwnProfileButtons()
        } else {
            properties["profileId"] = userId
            properties["userId"] = AuthenticationHelper.currentUserId
            analyticsHelper.logEvent("profile_viewed", properties)
            followBtn?.setOnClickListener {
                viewModel.toggleFollow()
            }
            friendBtn?.setOnClickListener {
                viewModel.toggleFriendship()
            }
            initOtherProfileButtons()
        }

        viewMore?.setOnClickListener {
            if (::userInfoForAbout.isInitialized) {
                val action = UserAboutFragmentDirections.showUserAboutFragment(userInfoForAbout)
                action.userId = userId.toLong()
                findNavController().navigate(action)
            }
        }

        addPostBtn?.setOnClickListener {
            openPostFragment()
        }

        bankBtn?.setOnClickListener {
            openBankFragment()
        }

    }


    private fun openPostFragment() {
        val navController = findNavController()
        navController.navigate(PostTypesFragmentDirections.showPostDraftFragment(null, null, null))
    }

    private fun openBankFragment() {
        val navController = findNavController()
        navController.navigate(R.id.bankAccountFragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isCurrentUserProfile) {
                    findNavController().navigate(R.id.userSettingsFragment, null, navOptions {
                        anim {
                            enter = R.anim.slide_enter_ltr
                            exit = R.anim.slide_exit_ltr
                            popEnter = R.anim.slide_pop_enter_ltr
                            popExit = R.anim.slide_pop_exit_ltr
                        }
                    })
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onResume() {
        super.onResume()
        initViewPagerAdapter(userId, view)
    }

    override fun onDestroy() {
        if (::userProfileImageService.isInitialized)
            userProfileImageService.onDestroy()
        super.onDestroy()
    }

    private fun initActionButton(ownProfile: Boolean, id: Int = -1, blocked: Boolean = false) {
        actionBtn?.let { button ->
            if (ownProfile) {
                button.setImageResource(R.drawable.ic_user_profile_edit)
            } else {
                button.setImageResource(R.drawable.ic_more_horizontal)
            }

            if (ownProfile) {
                button.setOnClickListener {
                    findNavController().navigate(R.id.showEditProfileFragment)
                }
            } else {
                button.setOnClickListener {
                    showMoreActionSheet(blocked, userId, currentFollowStatus, currentFriendStatus)
                }
            }


        }
    }

    fun collapseToolbar() {
        appBar!!.setExpanded(false)
    }

    private fun updateMessageBtn(userProfile: UserProfile, canMessage: Boolean) {
        if (canMessage && !isCurrentUserProfile) {
            messageBtn.show()
            messageBtn?.setOnClickListener {
                viewModel.userService.getCachedUserInfo(userId) ?: return@setOnClickListener
                findParentNavController().navigate(
                    R.id.chatFragment,
                    bundleOf("recipient" to userProfile.toPostUser())
                )
            }
        } else {
            messageBtn.hide()
        }
    }

    private fun initViewPagerAdapter(userId: Int, view: View?) {
        val viewPager = viewPager ?: return
        val activity = activity ?: return
        val supportFragmentManager = activity.supportFragmentManager

        if (viewPager.adapter == null) {
            val pagerAdapter = UserProfileTabsPagerAdapter(
                activity,
                userId,
                supportFragmentManager,
                this,
                Color.BLACK
            )

            viewPager.adapter = pagerAdapter

            tabLayout?.let { tabLayout ->

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    when (position) {
                        //0 -> tab.text = getString(R.string.user_profile_tab_about)
                        0 -> tab.text = getString(R.string.user_profile_tab_posts)
                        1 -> tab.text = getString(R.string.user_profile_tab_photos)
                        2 -> tab.text = getString(R.string.user_profile_tab_videos)
                    }
                }.attach()

                for (i in 0 until tabLayout.tabCount) {
                    tabLayout.getTabAt(i)?.setCustomView(R.layout.tab_item)
                }
                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab?) = Unit

                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                        setTabUnselected(tab)
                    }

                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        setTabSelected(tab)
                        currentTab = tab?.position ?: 0
                        if (viewModel.isUserBlocked) {
                            snack(R.string.user_is_blocked)
                        }
                    }
                })
                val tabPosition = if (currentTab != 0) currentTab else args.defaultTab
                viewPager.setCurrentItem(tabPosition, false)
                val tabToSelect = tabLayout.getTabAt(tabPosition)
                tabLayout.selectTab(tabToSelect)
                setTabSelected(tabToSelect)
            }
        }
    }

    private fun updateUserInfo(userInfo: UserProfile?) {
        if (context == null || userInfo == null) return
        updateName(userInfo.fullName)
        updateProfileName(userInfo.username)
        updateFollowerFriendCount(
            userInfo.followerCount,
            userInfo.friendCount,
            userInfo.followingCount
        )
        updateUserImages(userInfo)
        updateBlockStatus(userInfo.id, userInfo.isBlocked)
        updateMessageBtn(userInfo, userInfo.canMessage as Boolean)
        updateProfileButtons(userInfo)
        updateAboutSection(userInfo)
        user_profile_status_icon?.visibility =
            if (Constants.isVerified(userInfo.verifiedStatus)) View.VISIBLE else View.GONE

        userInfo.badges?.let {
            if (it.isNotEmpty()) {
                groupBadge.show()
                val badge = it[0].title
                badge?.let {
                    when (badge.toLowerCase(Locale.ENGLISH)) {
                        BadgesStatus.GOLD.toString().toLowerCase(Locale.ENGLISH) -> {
                            ivBadge?.setImageResource(R.drawable.ic_sup_goldandroid)
                            tvBadge?.text = activity?.getString(R.string.gold_user_badge)
                        }
                        BadgesStatus.PLATINUM.toString().toLowerCase(Locale.ENGLISH) -> {
                            ivBadge?.setImageResource(R.drawable.ic_sup_platandroid)
                            tvBadge?.text = activity?.getString(R.string.platinum_user_badge)
                        }
                        BadgesStatus.DIAMOND.toString().toLowerCase(Locale.ENGLISH) -> {
                            ivBadge?.setImageResource(R.drawable.ic_sup_diamandroid)
                            tvBadge?.text = activity?.getString(R.string.diamond_user_badge)
                        }
                        else -> {
                            groupBadge.hide()
                        }
                    }
                }
            } else {
                groupBadge?.hide()
            }
        } ?: kotlin.run {
            groupBadge?.hide()
        }

        if (isCurrentUserProfile.not()) {
            userInfo.isFriendsPrivate?.let {
                view?.findViewById<View>(R.id.user_info_friends_container)?.isEnabled =
                    userInfo.isFriendsPrivate.not()
            } ?: kotlin.run {
                view?.findViewById<View>(R.id.user_info_friends_container)?.isEnabled = true
            }

            userInfo.isFollowersPrivate?.let {
                view?.findViewById<View>(R.id.user_info_followers_container)?.isEnabled =
                    userInfo.isFollowersPrivate.not()
            } ?: kotlin.run {
                view?.findViewById<View>(R.id.user_info_followers_container)?.isEnabled = true
            }


            userInfo.isFollowingPrivate?.let {
                view?.findViewById<View>(R.id.user_info_following_container)?.isEnabled =
                    userInfo.isFollowingPrivate.not()
            } ?: kotlin.run {
                view?.findViewById<View>(R.id.user_info_following_container)?.isEnabled = true
            }
        }
    }


    private data class AboutSectionItem(
        val icon: Int,
        val label: String?,
        val value: String?,
        val valueClick: ((View) -> Unit)? = null
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val color = resources.getColor(R.color.screen_background)

        appBar?.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val bottomPos = user_profile_name?.top?.toFloat() ?: 1f
            val pos = Math.abs(verticalOffset)
            val alpha = (pos / bottomPos).coerceAtMost(1f)
            val newColor = ColorUtils.setAlphaComponent(color, (255 * alpha).toInt())
            toolbar.setBackgroundColor(newColor)
            toolbarTitle.setVisibleOrGone(alpha == 1f)
        })

        btn_toolbar_search?.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }

        btn_toolbar_notify.setOnClickListener {
            findNavController().navigate(R.id.notificationFragment)
        }
    }

    private fun updateAboutSection(userInfo: UserProfile) {
        userInfoForAbout = userInfo.toProfileParams()
        viewMore.show()

        val infoItems: List<AboutSectionItem> = createAboutItems(userInfo)

        when (infoItems.size) {
            0 -> {
                user_info_row1.hide()
                user_info_row2.hide()
            }
            1 -> {
                user_info_row1.show()
                user_info_row2.hide()
                fillRow1(infoItems[0])
            }
            else -> {
                user_info_row1.show()
                user_info_row2.show()
                fillRow1(infoItems[0])
                fillRow2(infoItems[1])
            }
        }
    }

    private fun fillRow1(item: AboutSectionItem) {
        fillRow(item, info_row1_icon, info_row1_label, info_row1_text)
    }

    private fun fillRow2(item: AboutSectionItem) {
        fillRow(item, info_row2_icon, info_row2_label, info_row2_text)
    }

    private fun fillRow(item: AboutSectionItem, icon: ImageView, label: TextView, value: TextView) {
        if (!item.label.isNullOrEmpty()) {
            label.text = item.label
            label.show()
        } else {
            label.hide()
        }

        value.text = item.value?.trim()
        item.valueClick?.let { onClick ->
            value.paintFlags = value.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            value.setOnClickListener(onClick)
        }

        icon.setImageResource(item.icon)
    }

    private fun createAboutItems(userInfo: UserProfile): List<AboutSectionItem> {
        val list = ArrayList<AboutSectionItem>()

        if (!userInfo.bio.isNullOrEmpty()) {
            list.add(AboutSectionItem(R.drawable.user_nav_header, null, userInfo.bio))
        }
        if (!userInfo.website.isNullOrEmpty()) {
            list.add(AboutSectionItem(R.drawable.ic_website, null, userInfo.website))
        }
        val locationName = userInfo.currentLocation?.locationName
        if (!locationName.isNullOrEmpty()) {
            list.add(AboutSectionItem(R.drawable.user_nav_header, "From", locationName))
        }
        if (!userInfo.relationshipStatus.isNullOrEmpty()) {
            val label = if (!userInfo.relationshipWith.isNullOrBlank()) {
                getString(R.string.user_info_relationship_to, userInfo.relationshipStatus)
            } else {
                userInfo.relationshipStatus
            }
            list.add(
                AboutSectionItem(R.drawable.ic_relationship, label, userInfo.relationshipWith) {
                    findNavController().showUserProfile(userInfo.relationshipWithId)
                }
            )
        }



        return list
    }

    private fun updateProfileButtons(userInfo: UserProfile) {
        if (isCurrentUserProfile) {
            //own profile
            initOwnProfileButtons()

        } else {
            //other user profile
            initOtherProfileButtons()
        }
    }

    private fun updateBlockStatus(userId: Int, blocked: Boolean) {
        initActionButton(isCurrentUserProfile, userId, blocked)
        /*actionBtn?.let { button ->
            button.setImageResource(R.drawable.ic_user_profile_edit)
            button.setOnClickListener {
                showMoreActionSheet(blocked, userId)
            }
        }*/
    }

    private fun initOwnProfileButtons() {
        ownButtonsContainer.show()
        otherButtonsContainer.hide()
    }

    private fun initOtherProfileButtons() {
        ownButtonsContainer.hide()
        otherButtonsContainer.show()
    }

//    private fun updateBlockStatus(userId: Int, blocked: Boolean) {
//        moreBtn?.setOnClickListener {
//            val sheetView = activity?.layoutInflater?.inflate(
//                R.layout.dialog_bottom_other_users_more_actions,
//                null
//            )
//            val actionSheet = BottomSheetDialog(context as Context)
//            actionSheet.setContentView(sheetView as View)
//            actionSheet.show()
//        }
//    }

    private fun showMoreActionSheet(
        blocked: Boolean,
        userId: Int,
        isFollowing: Boolean,
        friendStatus: UserFriendshipStatus
    ): Unit? {
        val sheetView =
            activity?.layoutInflater?.inflate(R.layout.dialog_bottom_other_users_more_actions, null)
        val actionSheet = BottomSheetDialog(context as Context, R.style.ProfileSheetDialog)
        actionSheet.setContentView(sheetView as View)
        actionSheet.show()

        sheetView.findViewById<View>(R.id.dialog_cancel_button)?.setOnClickListener {
            actionSheet.dismiss()
        }

        sheetView.findViewById<View>(R.id.dialog_unfollow_button)?.apply {
            setOnClickListener {
                viewModel.toggleFollow()
                actionSheet.dismiss()
            }
            findViewById<TextView>(R.id.dialog_unfollow_button_text)?.text =
                getText(if (isFollowing) R.string.btn_unfollow_txt else R.string.btn_follow_txt)
        }

        sheetView.findViewById<ViewGroup>(R.id.dialog_report_button)?.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val reasons = resources.getStringArray(R.array.report_reasons_array)
            builder.setTitle("Reason")
            builder.setNegativeButton(getString(R.string.cancel)) { _, _ ->
                actionSheet.dismiss()
            }
            builder.setItems(reasons) { _, which ->
                when (which) {
                    reasons.lastIndex -> {
                        val userProfile = viewModel.userProfileLiveData.value
                        userProfile?.let {
                            val url = NetworkConstants.COPYRIGHT_URL_FORMAT.format(
                                null,
                                userProfile.firstname,
                                userProfile.lastname
                            )
                            findNavController().navigate(
                                R.id.copyrightInfringement,
                                bundleOf("url" to url)
                            )
                        }
                    }
                    else -> viewModel.reportUser(userId, which + 1)
                }
            }

            builder.create().show()

            actionSheet.dismiss()
        }
        val buttonText = if (blocked) getString(R.string.unblock_user) else getString(R.string.block_user)
        sheetView.findViewById<TextView>(R.id.dialog_block_text)?.text = buttonText
        sheetView.findViewById<View>(R.id.dialog_block_button)?.setOnClickListener {
            viewModel.toggleUserBlock(userId, blocked)
        }

        sheetView.findViewById<View>(R.id.dialog_unfriend_button)?.apply {
            setOnClickListener {
                viewModel.toggleFriendship()
            }
        }
        sheetView.findViewById<TextView>(R.id.dialog_unfriend_button_text)?.text =
            getText(if (friendStatus == UserFriendshipStatus.ACCEPTED) R.string.unfriend else R.string.add_friend)

        return sheetView.findViewById<View>(R.id.dialog_block_button)?.setOnClickListener {
            viewModel.toggleUserBlock(userId, blocked)
            actionSheet.dismiss()
        }
    }

    private fun setTabSelected(tab: TabLayout.Tab?) {
        tab?.customizeTitle { textView ->
            textView.typeface = ResourcesCompat.getFont(textView.context, R.font.lato_bold)
        }
    }

    private fun setTabUnselected(tab: TabLayout.Tab?) {
        tab?.customizeTitle { textView ->
            textView.typeface = ResourcesCompat.getFont(textView.context, R.font.lato_medium)
        }
    }

    private fun updateUserImages(userInfo: UserProfile) {
        userProfileImageService.getProfilePicture(userInfo.profilePictureUrl, false) {
            if (isCurrentUserProfile) {
                updateProfileIcon(it)
            }
            it?.let {
                userProfilePhoto?.setImageDrawable(it)
            } ?: run {
                userProfilePhoto?.setImageResource(R.drawable.user)
            }
        }

        userProfileImageService.getCoverPicture(userInfo.coverPictureUrl, false) {
            it?.let {
                coverPhoto?.setImageDrawable(it)
            } ?: run {
                coverPhoto?.setImageResource(R.drawable.ic_no_cover_small)
            }
        }
    }

    private fun updateOtherUserButtons() {
        if (currentFollowStatus && currentFriendStatus == UserFriendshipStatus.ACCEPTED) {
            messageBtn?.text = getText(R.string.user_profile_message)
            messageBtn?.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 1f
                width = LinearLayout.LayoutParams.MATCH_PARENT
                marginStart = 0
            }

            supportBtn?.text = getText(R.string.support_me_button_label)
            supportBtn?.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 1f
                width = LinearLayout.LayoutParams.MATCH_PARENT
            }
        } else {
            messageBtn?.text = null
            messageBtn?.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 0f
                width = LinearLayout.LayoutParams.WRAP_CONTENT
                marginStart = resources.getDimensionPixelSize(R.dimen.user_profile_button_margin)
            }

            supportBtn?.text = null
            supportBtn?.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 0f
                width = LinearLayout.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun updateFriendStatus(friendStatus: UserFriendshipStatus) {
        if (context == null) return
        this.currentFriendStatus = friendStatus
        val friendButton = friendBtn ?: return
        friendButton.show()

        Log.i("userProfile", "update friend status: $friendStatus")
        when (friendStatus) {
            UserFriendshipStatus.REQUESTED -> {
                friendButton.text = getString(R.string.respond)
                context?.let {
                    friendButton.icon = it.getDrawable(R.drawable.ic_friendship_accept)
                }
                setButtonOn(friendButton)
            }
            UserFriendshipStatus.PENDING -> {
                friendButton.text = getString(R.string.cancel)
                context?.let {
                    friendButton.icon = it.getDrawable(R.drawable.ic_cancel_friend_request)
                }
                setButtonOff(friendButton)
            }
            UserFriendshipStatus.ACCEPTED -> {
                friendButton.hide()
            }
            else -> {
                // not friends
                context?.let {
                    friendButton.icon = it.getDrawable(R.drawable.ic_friend)
                }
                friendButton.text = getString(R.string.add_friend)
                setButtonOn(friendButton)
            }
        }

        updateOtherUserButtons()
    }

    private fun updateFollowStatus(following: Boolean) {
        if (context == null) return
        val followButton = followBtn ?: return

        currentFollowStatus = following

        followButton.show()
        if (following) {
            followButton.text = getString(R.string.btn_unfollow_txt)
            followButton.icon = null
            setButtonOff(followButton)
            followButton.hide()
        } else {
            followButton.text = getString(R.string.btn_follow_txt)
            context?.let {
                followButton.icon = it.getDrawable(R.drawable.ic_follow)
            }
            setButtonOn(followButton)
        }

        updateOtherUserButtons()
    }

    private fun setButtonOn(button: MaterialButton) {
        val context = context ?: return
        button.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.button_background_enabled)
    }

    private fun setButtonOff(button: MaterialButton) {
        val context = context ?: return
        button.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.button_background_decline)
    }

    private fun updateName(fullName: String) {
        user_profile_name?.text = fullName
        toolbarTitle.text = fullName
    }

    private fun updateProfileName(name: String) {
        user_profile_handle?.text = "@$name"
    }

    private fun formatNumber(number: Int): String? {
        return if (number > 9999) "10k+" else numberFormat.format(number)
    }

    private fun updateFollowerFriendCount(
        followerCount: Int,
        friendCount: Int,
        followingCount: Int
    ) {
        followerCountLabel.show()
        followingCountLabel.show()
        friendCountLabel.show()
        followersCountText?.text = formatNumber(followerCount)
        followingCountText?.text = formatNumber(followingCount)
        friendsCountText?.text = formatNumber(friendCount)
    }

}