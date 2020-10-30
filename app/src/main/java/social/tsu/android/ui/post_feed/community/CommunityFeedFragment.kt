package social.tsu.android.ui.post_feed.community

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_community_feed.*
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.models.TsuNotificationType
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.helper.ads.GoogleAdFetcher
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.Membership
import social.tsu.android.network.model.PendingPost
import social.tsu.android.network.model.Role
import social.tsu.android.notifications.FCMService
import social.tsu.android.ui.community.*
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.messaging.tsu_contacts.TsuContactsFragment
import social.tsu.android.ui.messaging.tsu_contacts.TsuContactsFragmentArgs
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.new_post.PostDraftFragmentDirections
import social.tsu.android.ui.new_post.PostDraftType
import social.tsu.android.ui.post_feed.BaseFeedFragment
import social.tsu.android.ui.post_feed.UserPostsAdapter
import social.tsu.android.ui.setVisibleOrGone
import social.tsu.android.ui.util.RetryCallback
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import javax.inject.Inject


class CommunityFeedFragment constructor() : BaseFeedFragment<CommunityFeedViewModel>(),
    RetryCallback, UserPostsAdapter.ViewHolderActions,
    CommunityCreateListener {

    constructor(communityId: Int) : this() {
        this.communityId = communityId
    }

    private var motionProgress: Float = 0f
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var joinButton: MaterialButton
    private lateinit var motionLayout: MotionLayout
    private lateinit var progressBar: ProgressBar

    private var communityId: Int = 0
    private var community: Group? = null
    private var membership: Membership? = null
    private var optionsMenuItem: MenuItem? = null
    private var bottomSheetDialog: BottomSheetDialog? = null

    private var bottomListener: BottomListener? = null

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val viewModel by viewModels<CommunityFeedViewModel> { viewModelFactory }
    private val model: CommunityViewModel by activityViewModels()

    override val postsAdapter by lazy {
        UserPostsAdapter(
            activity?.application as TsuApplication,
            exoPlayer,
            this,
            this,
            addPostComposeView = membership != null,
            useMiniComposePost = true,
            showCommunityInTitle = false,
            nativeAdFetcher = GoogleAdFetcher.getInstance(
                "community_$communityId",
                requireActivity()
            )
        )
    }

    private val compositeDisposable = CompositeDisposable()
    private val args by navArgs<CommunityFeedFragmentArgs>()

    @Inject
    lateinit var api: PostApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.communityListeners.add(this)
        setHasOptionsMenu(true)


    }

    override fun onDestroy() {
        super.onDestroy()
        model.communityListeners.remove(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("progress", community_holder?.progress ?: 0f)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bottomListener = context as BottomListener
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(
            R.layout.fragment_community_feed,
            container, false
        )

        progressBar = view.findViewById(R.id.progress_bar)
        val progressMain: ProgressBar = view.findViewById(R.id.progress_main)
        progressCommunity = progressMain

        joinButton = view.findViewById(R.id.community_join_btn)
        joinButton.setOnClickListener {
            community?.let {
                joinButton.hide()
                Log.d("testthis", "it is:" + membership?.invitedById)
                if (membership?.invitedById != null) {
                    viewModel.acceptMembership(membership!!.id)
                        .observe(viewLifecycleOwner, Observer { result ->
                            handleLoadState(result) {
                                loadMembership()
                                viewModel.refreshCommunityPosts(communityId)
                            }
                        })

                } else {
                    viewModel.joinCommunity(it).observe(viewLifecycleOwner, Observer { result ->
                        handleLoadState(result) {
                            loadMembership()
                            viewModel.refreshCommunityPosts(communityId)
                        }
                    })
                }
            }
        }

        processArgs()

        swipeRefreshLayout = view.findViewById(R.id.community_swipe_refresh)
        swipeRefreshLayout.setOnRefreshListener {
            loadMembership()
            updateFeed()
        }

        motionLayout = view.findViewById<MotionLayout>(R.id.community_holder)

        motionLayout.progress = motionProgress


        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            layoutManager = postsLayoutManager
            adapter = postsAdapter
        }

        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        viewModel.postsLoadState.observe(viewLifecycleOwner, Observer {
            postsAdapter::setLoadState
        })

        postsAdapter.onItemRemove ={
            progressMain.hide()
        }

        viewModel.getCommunityPosts(communityId)
            .observe(viewLifecycleOwner, Observer(postsAdapter::submitList))

        viewModel.userRefreshLoadState.observe(viewLifecycleOwner, Observer {
            swipeRefreshLayout.isRefreshing = it is Data.Loading
            handleLoadState(it) {}
        })

        updatePublishingRequests()

        FCMService.lastNotificationData.observe(viewLifecycleOwner, Observer { messageData ->
            if (messageData.resource?.id == communityId.toString()) {
                when (messageData.type) {
                    TsuNotificationType.PENDING_POST_IN_CHANNEL_QUEUE -> {
                        updatePublishingRequests()
                    }
                    TsuNotificationType.GROUP_MEMBERSHIP_APPROVAL,
                    TsuNotificationType.GROUP_MEMBERSHIP_REQUEST -> {
                        loadMembership()
                    }
                }
            }
        })

        return view
    }

    override fun <T> handleLoadState(loadState: Data<T>, onSuccess: (data: T) -> Unit) {
        if (loadState is Data.Loading) {
            progressBar.show()
        } else {
            progressBar.hide()
        }
        super.handleLoadState(loadState, onSuccess)
    }

    private fun loadMembership() {
        viewModel.loadMembership(communityId).observe(viewLifecycleOwner, Observer { data ->
            handleLoadState(data) { memberships ->
                didLoadMembership(memberships.find { it.group.id == communityId })
            }
        })
    }

    private fun updateFeed() {
        if (requireActivity().isInternetAvailable()) {
            viewModel.refreshCommunityPosts(communityId, true)
            updatePublishingRequests()
        } else
            requireActivity().internetSnack()
    }

    private fun updatePublishingRequests() {
        if (membership?.getRole(AuthenticationHelper.currentUserId ?: 0) == Role.OWNER) {
            viewModel.getPendingPosts(communityId).observe(viewLifecycleOwner, Observer {
                handleLoadState(it) { pendingPosts ->
                    didLoadPendingPosts(pendingPosts)
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.community_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        optionsMenuItem = menu.findItem(R.id.community_options)
        updateOptionsMenu()
    }

    private fun updateOptionsMenu() {
        val membership = this.membership
        if (membership == null) {
            optionsMenuItem?.isVisible = false
            return
        }

        val role = membership.getRole(AuthenticationHelper.currentUserId ?: 0)
        val statusAccepted = membership.getStatus() == Membership.Status.ACCEPTED
        optionsMenuItem?.isVisible = role != Role.NONE && statusAccepted
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.community_options) {
            showBottomSheetMenu()
        }
        return false
    }

    private fun showBottomSheetMenu() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val dialogView = bottomSheetDialog.layoutInflater.inflate(R.layout.community_options, null)
        bottomSheetDialog.setContentView(dialogView)

        bindMembershipToDialogView(bottomSheetDialog)
        bottomSheetDialog.setOnDismissListener {
            this.bottomSheetDialog = null
        }
        bottomSheetDialog.setOnCancelListener {
            this.bottomSheetDialog = null
        }

        this.bottomSheetDialog = bottomSheetDialog
        bottomSheetDialog.show()
    }

    private fun bindMembershipToDialogView(bottomSheetDialog: BottomSheetDialog) {
        val role = membership?.getRole(AuthenticationHelper.currentUserId ?: 0) ?: Role.NONE
        when (role) {
            Role.OWNER -> {
                bottomSheetDialog.findViewById<TextView>(R.id.role)?.setText(R.string.role_owner)
                bottomSheetDialog.findViewById<TextView>(R.id.publishing_requests_count)?.apply {
                    if (viewModel.pendingPosts.isNullOrEmpty()) {
                        hide()
                    } else {
                        show()
                        text = viewModel.pendingPosts?.size.toString()
                    }
                }
                bottomSheetDialog.findViewById<View>(R.id.publishing_requests_group)
                    ?.setOnClickListener {
                        findNavController().navigate(
                            R.id.action_global_open_communityPublishingRequestsFragment,
                            bundleOf("groupId" to communityId)
                        )
                        bottomSheetDialog.dismiss()
                    }
            }
            Role.ADMIN -> {
                bottomSheetDialog.findViewById<TextView>(R.id.role)?.setText(R.string.role_admin)
                bottomSheetDialog.findViewById<View>(R.id.settings_button)?.setVisibleOrGone(false)
            }
            Role.MEMBER -> {
                bottomSheetDialog.findViewById<TextView>(R.id.role)?.setText(R.string.role_member)
                bottomSheetDialog.findViewById<View>(R.id.settings_button)?.setVisibleOrGone(false)
                bottomSheetDialog.findViewById<View>(R.id.members_requests_group)
                    ?.setVisibleOrGone(false)
                bottomSheetDialog.findViewById<View>(R.id.publishing_requests_group)
                    ?.setVisibleOrGone(false)
            }
            Role.NONE -> {
            }
        }

        bottomSheetDialog.findViewById<TextView>(R.id.leave_button)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setMessage("Are you sure you want to leave ${membership?.group?.name}")
                .setPositiveButton(R.string.leave) { dialogInterface, i ->
                    viewModel.leave(membership?.id ?: 0).observe(viewLifecycleOwner, Observer {
                        handleLoadState(it) {
                            didLeaveGroup()
                        }
                    })
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialogInterface, i -> dialogInterface.cancel() }
                .show()
        }

        val requestsCount = membership?.group?.requestsCount

        bottomSheetDialog.findViewById<TextView>(R.id.members_requests_count)?.text =
            if (requestsCount != null && requestsCount > 0) {
                requestsCount.toString()
            } else {
                null
            }

        bottomSheetDialog.findViewById<View>(R.id.members)?.setOnClickListener {
            findNavController().navigate(
                R.id.communityMembersFragment, bundleOf(
                    "id" to (community?.id ?: 0),
                    "role_type" to role.toString()
                )
            )
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<TextView>(R.id.invite_button)?.setOnClickListener {
            findNavController().navigate(
                R.id.tsuContactsFragment, TsuContactsFragmentArgs.Builder(
                    TsuContactsFragment.ContactsMode.COMMUNITY_INVITE
                ).setCommunityId(
                    communityId
                ).build().toBundle()
            )
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<View>(R.id.members_requests_group)?.setOnClickListener {
            community?.let {
                findNavController().navigate(
                    R.id.pendingMembershipFragment, PendingMembershipFragmentArgs.Builder(
                        it
                    ).build().toBundle()
                )
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.findViewById<TextView>(R.id.settings_button)?.setOnClickListener {
            val action = CommunityCreateFragmentDirections.actionGlobalCommunityCreateFragment()
                .apply {
                    mode =
                        CommunityCreateFragment.CreateCommunityMode.EDIT
                    community = this@CommunityFeedFragment.community ?: membership?.group
                }
            findNavController().navigate(action)
            bottomSheetDialog.dismiss()
        }
    }

    private fun setupHeader(view: View) {
        val communityName = view.findViewById<TextView>(R.id.community_name)
        val communityType = view.findViewById<TextView>(R.id.community_type)
        val communityMembers = view.findViewById<TextView>(R.id.community_members_text)
        val communityDescription = view.findViewById<ExpandableTextView>(R.id.community_description)
        val moreText = view.findViewById<TextView>(R.id.community_more)
        val communityBanner = view.findViewById<ImageView>(R.id.community_banner)
        val communityHolder = view.findViewById<MotionLayout>(R.id.community_holder)
        val communityHeader = view.findViewById<ConstraintLayout>(R.id.community_holder)
        val communityModerationIcon = view.findViewById<ImageView>(R.id.moderation_image)

        communityModerationIcon?.setVisibleOrGone(community?.requireModeration ?: false)

        communityName.text = community?.name

        val title = when (community?.visibility) {
            "open" -> resources.getString(R.string.community_label_public)
            "restricted" -> resources.getString(R.string.community_label_private)
            "exclusive" -> resources.getString(R.string.community_label_exclusive)
            else -> resources.getString(R.string.community_label_private)
        }

        val moderationText =
            if (community?.requireModeration == true) getString(R.string.community_moderated) else ""

        communityType.text =
            TSUTextTokenizingHelper.makeCommunityTitle(requireContext(), title, moderationText)

        communityDescription.text = community?.description
        communityDescription.setInterpolator(DecelerateInterpolator())
        communityDescription.post {
            if (!isTextViewEllipsized(communityDescription)) {
                moreText.visibility = View.GONE
            }
        }

        moreText.setOnClickListener {
            moreText.setText(if (communityDescription.isExpanded) R.string.community_more else R.string.community_less)
            communityDescription.toggle()
        }

        val membersString = resources.getQuantityString(
            R.plurals.members_quantity_no_number,
            community?.membersCount ?: 0,
            community?.membersCount
        )

        communityMembers.text = TSUTextTokenizingHelper.makeCommunityCounter(
            context,
            community?.membersCount ?: 0,
            membersString,
            membership?.group?.requestsCount ?: 0
        )
        communityMembers.setOnClickListener {
            val role = membership?.getRole(AuthenticationHelper.currentUserId ?: 0) ?: Role.NONE
            findNavController().navigate(
                R.id.communityMembersFragment, bundleOf(
                    "id" to (community?.id ?: 0),
                    "role_type" to role.toString()
                )
            )
        }


        val cornerRadius = resources.getDimensionPixelSize(R.dimen.community_picture_corners)

        community?.pictureUrl?.let {
            val url = it.split("/groups").last()
            Glide.with(communityBanner.context).load(formatUrl("/groups$url")).into(communityBanner)
        }

        communityHolder.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > communityHeader.height) {
                Log.d("CommunityFeed", "should set title!")
            }
        }

        communityHolder.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
            }

            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
                Log.d("CommunityFeed", "onTransitionChange $p1 $p2 $p3")
                swipeRefreshLayout.isEnabled = p3 == 0f
                if (p3 > 0.5f) {
                    (requireActivity() as AppCompatActivity).supportActionBar?.title =
                        community?.name
                } else {
                    (requireActivity() as AppCompatActivity).supportActionBar?.title = ""
                }
            }

            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
            }

        })

    }

    /**
     * Checks if the text of the supplied [TextView] has been ellipsized.
     *
     * @param textView
     * The [TextView] to check its text.
     *
     * @return `True` if the text of the supplied `textView` has been ellipsized.
     */
    private fun isTextViewEllipsized(textView: TextView?): Boolean {
        // Check if the supplied TextView is not null
        if (textView == null) {
            return false
        }

        // Check if ellipsizing the text is enabled
        val truncateAt: TextUtils.TruncateAt? = textView.ellipsize
        if (truncateAt == null || TextUtils.TruncateAt.MARQUEE.equals(truncateAt)) {
            return false
        }

        // Retrieve the layout in which the text is rendered
        val layout = textView.layout ?: return false

        // Iterate all lines to search for ellipsized text
        for (line in 0 until layout.lineCount) {

            // Check if characters have been ellipsized away within this line of text
            if (layout.getEllipsisCount(line) > 0) {
                return true
            }
        }
        return false
    }

    private fun initMoreButton(
        communityDescription: TextView,
        moreButton: TextView, maxLines: Int
    ) {
        if (communityDescription.lineCount > maxLines) {
            moreButton.visibility = View.VISIBLE
            moreButton.setOnClickListener {
                cycleTextViewExpansion(communityDescription, maxLines)
            }
        } else {
            moreButton.visibility = View.GONE
        }
    }

    private fun cycleTextViewExpansion(tv: TextView, collapsedMaxLines: Int) {
        val animation = ObjectAnimator.ofInt(
            tv, "maxLines",
            if (tv.maxLines == collapsedMaxLines) tv.lineCount else collapsedMaxLines
        )
        animation.setDuration(200).start()
    }


    private fun formatUrl(source: String): String {
        if (source.startsWith("/")) {
            return "${HostProvider.imageHost}${source}".replace("square", "cover")
        }

        return source
    }

    private fun processArgs() {
        args.membership?.let { argMembership ->
            membership = argMembership
            memberShipRole = argMembership
            communityId = argMembership.group.id
            if (community == null) {
                community = argMembership.group
            }
        } ?: args.group?.let { group ->
            communityId = group.id
            if (community == null) {
                community = group
            }
        } ?: run {
            communityId = args.groupId
        }

        if (community == null) {
            viewModel.loadCommunity(communityId).observe(viewLifecycleOwner, Observer {
                handleLoadState(it) { group ->
                    didLoadGroup(group)
                    didLoadMembership(group.membership)
                }
            })
        }
        if (membership == null) {
            loadMembership()
        } else {
            updateMembershipStatus()
        }
    }

    override fun onDestroyView() {
        compositeDisposable.dispose()
        motionProgress = motionLayout.progress
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCommunityPosts(communityId)
        view?.let {
            setupHeader(it)
        }
        recyclerView.let {
            it.smoothScrollBy(0, -1)
        }
    }

    override fun onStart() {
        super.onStart()
        bottomListener?.communityClick()
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = ""
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = ""
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun didTapPost() {
        //Start PostDraftFragment and setup it for posting in community that user wanted to add post into
        val direction = PostDraftFragmentDirections.showPostDraftFragment(null, null, null)
            .apply {
                membership = this@CommunityFeedFragment.membership
                memberShipRole = this@CommunityFeedFragment.membership
                postingType = PostDraftType.COMMUNITY
                popToDestination = R.id.communityFeedFragment
            }
        findNavController().navigate(direction)
    }

    override fun retry() {
        viewModel.retry()
    }

    private fun didLoadGroup(group: Group) {
        this.community = group
        view?.let { setupHeader(it) }
    }

    private fun didLoadMembership(membership: Membership?) {
        this.membership = membership
        this.memberShipRole = membership
        bottomSheetDialog?.let { bottomDialog ->
            bindMembershipToDialogView(bottomDialog)
        }

        val addPostComposeView = membership != null
        if (postsAdapter.addPostComposeView != addPostComposeView) {
            postsAdapter.addPostComposeView = addPostComposeView
            postsAdapter.notifyDataSetChanged()
        }
        updateOptionsMenu()
        updateMembershipStatus()
    }

    private fun didLeaveGroup() {
        findNavController().popBackStack()
    }

    private fun didLoadPendingPosts(posts: List<PendingPost>) {
        viewModel.pendingPosts = posts
    }

    private fun toast(@StringRes strRes: Int) {
        snack(strRes)
    }

    override fun onCommunityCreated() {
        //not interested in this event
    }

    override fun onCommunityChanged(group: Group) {
        community = group
        view?.let { setupHeader(it) }
    }

    override fun onCommunityDeleted() {
    }

    override fun refreshPosts() {
        viewModel.refreshCommunityPosts(communityId)
    }

    override fun didTapHashtag(hashtag: String) {
        findNavController().navigate(
            R.id.hashtagGridFragment, bundleOf(
                "hashtag" to hashtag
            )
        )
    }

    override fun openMentionSearchFragment(composedText: String,position: Int) {
        TODO("Not yet implemented")
    }

    private fun updateMembershipStatus() {
        when (membership?.getStatus()) {
            Membership.Status.PENDING -> {
                if (membership?.invitedById != null) {
                    joinButton.setText(R.string.requested)
                    joinButton.isEnabled = true
                } else {
                    joinButton.isEnabled = false
                    joinButton.setText(R.string.pending)
                }
                joinButton.show()
                joinButton.setIconResource(R.drawable.ic_pending)
            }
            null -> {
                joinButton.show()
                joinButton.isEnabled = true
                joinButton.setIconResource(R.drawable.ic_add)
                joinButton.setText(R.string.community_join)
            }
            else -> joinButton.hide()
        }
    }

}




