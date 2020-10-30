package social.tsu.android.ui.post_feed

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.android.support.AndroidSupportInjection
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.feed.*
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.NetworkConstants.COPYRIGHT_URL_FORMAT
import social.tsu.android.network.model.*
import social.tsu.android.network.model.PendingPost
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.*
import social.tsu.android.ui.AdsSupportViewModel
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.AdContent
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


abstract class BaseFeedFragment<T : BaseFeedViewModel> : Fragment(),
    UserPostsAdapter.ViewHolderActions {

    private var toast: Toast? = null

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    @Inject
    lateinit var rxSchedulers: RxSchedulers

    private val adsSupportViewModel by activityViewModels<AdsSupportViewModel>()

    private var lastBottomDialog: BottomSheetDialog? = null

    val application by lazy { requireContext().applicationContext as TsuApplication }

    val properties = HashMap<String, Any?>()

    var memberShipRole: Membership? = null
    var progressCommunity: ProgressBar? = null
    var post: Post? = null


    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    protected abstract val postsAdapter: UserPostsAdapter
    protected abstract val viewModel: T

    val exoPlayer: SimpleExoPlayer by lazy {
        requireActivity().createAppExoPlayer()
    }

    val postsLayoutManager: PostFeedLayoutManager?
        get() = PostFeedLayoutManager(requireContext())

    private val userProfileImageService by lazy {
        DefaultUserProfileImageService(context?.applicationContext as TsuApplication)
    }

    private val userService: UserInfoService by lazy {
        DefaultUserInfoService(
            context?.applicationContext as TsuApplication,
            object : UserInfoServiceCallback {
                override fun completedGetUserInfo(info: UserProfile?) {}
                override fun didErrorWith(message: String) {}
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adsSupportViewModel.publicSupportEnabledLiveData.observe(viewLifecycleOwner, Observer {
            postsAdapter.isPublicSupportAdLoaded = it
            if (it.not()) {
                post?.let {
                    showCustomToast(it)
                }
            }
            postsAdapter.notifyDataSetChanged()
        })
        adsSupportViewModel.exclusiveSupportEnabledLiveData.observe(viewLifecycleOwner, Observer {
            postsAdapter.isExclusiveSupportAdLoaded = it
            if (it.not()) {
                post?.let {
                    showCustomToast(it)
                }
            }
            postsAdapter.notifyDataSetChanged()
        })
    }

    override fun onResume() {
        super.onResume()
        adsSupportViewModel.reloadSupportAds()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.playWhenReady = false
        exoPlayer.playbackState
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.stop()
        exoPlayer.release()
        postsAdapter.onDestroy()
    }

    override fun didTapLikeOn(post: Post) {
        if (requireActivity().isInternetAvailable().not())
            requireActivity().internetSnack()
        if (post.has_liked == true) {
            viewModel.unlike(post)
        } else {
            viewModel.like(post)
        }
    }

    override fun didTapShowLikes(post: Post) {
        findNavController().navigate(
            R.id.likesListFragment,
            bundleOf("postId" to post.originalPostId)
        )
    }

    override fun didTapMoreOptions(post: Post) {
        showExtraOptionsOn(post)
    }

    override fun didTapCommentOn(post: Post) {
        findNavController().navigate(R.id.commentsFragment, bundleOf("postId" to post.id))
    }

    override fun didTapOnUser(userId: Long) {
        findNavController().showUserProfile(userId.toInt())
    }

    override fun didTapOnGroup(groupId: Int) {
        findNavController().navigate(
            R.id.communityFeedFragment, bundleOf(
                "groupId" to groupId
            )
        )
    }

    override fun didTapLink(link: String) {
        context?.openUrl(link)
    }

    private fun showExtraOptionsOn(post: Post, blocked: Boolean = false) {
        if (lastBottomDialog?.isShowing == true) return

        if (post.user_id == AuthenticationHelper.currentUserId) {
            if (post.is_share && post.shared_id != null) {
                showSharedExtraDialog(post)
            } else {
                showUserPostExtraDialog(post)
            }
        } else if (post.has_shared == true && post.shared_id != null) {
            showSharedExtraDialog(post)
        } else {
            // No need to implement this now, it will be covered in ts-673 and ts-557
            showPostExtraDialog(post, blocked)
        }
    }

    override fun didTapOnShare(post: Post): LiveData<Data<Boolean>>? {
        if (requireActivity().isInternetAvailable().not())
            requireActivity().internetSnack()
        if (post.user_id == AuthenticationHelper.currentUserId) {
            snack(R.string.share_own_msg)
            return null
        }
        if (post.has_shared == true) {
            snack(R.string.share_shared_msg)
            return null
        }
        //TODO - Fix the below block to share only public posts
        if (!post.isSharable) {
            snack(R.string.create_post_private_msg)
            return null
        }
        val shareLD = viewModel.share(post)
        shareLD.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            handleLoadState(it) {

                if (post.groupId != null) {
                    properties["groupId"] = post.groupId
                }
                properties["postId"] = post.originalPostId
                properties["type"] = privacystring(post.privacy)
                properties["has_video"] = post.has_video
                properties["has_picture"] = post.has_picture
                properties["has_gif"] = post.has_gif
                if (!post.title.isEmpty()) {
                    properties["has_text"] = true
                }
                analyticsHelper.logEvent("post_shared", properties)
                snack(getString(R.string.post_successfully_shared))
            }
        })
        return shareLD
    }

    override fun didTapSupportButton(post: Post) {
        this.post = post
        adsSupportViewModel.didTapSupportButton(requireActivity(), post) {
            properties["postId"] = post.id
            if (post.privacy == 2) {
                properties["type"] = "exclusive"
                analyticsHelper.logEvent("exclusive_support_me_ad_viewed", properties)
            } else {
                analyticsHelper.logEvent("support_me_ad_viewed", properties)
            }
            postsAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCustomToast(post: Post, duration: Int = Toast.LENGTH_LONG) {
        toast?.cancel()
        activity?.let {
            val inflater = layoutInflater
            val layout: View = inflater.inflate(R.layout.support_toast_layout, null)
            toast = Toast(it)
            toast?.view = layout
            val ivProfile: CircleImageView = layout.findViewById(R.id.ivProfile)
            ivProfile.setImageResource(R.drawable.user)
            userProfileImageService.getProfilePicture(
                post.user.profilePictureUrl,
                false
            ) { dImage ->
                dImage?.let { image ->
                    ivProfile.setImageDrawable(image)
                }
            }
            val tvMessage = layout.findViewById(R.id.tvMessage) as AppCompatTextView
            val btnClose = layout.findViewById(R.id.btnClose) as AppCompatButton?

            btnClose?.setOnClickListener {
                toast?.cancel()
            }
            it.runOnUiThread {
                tvMessage.text =
                    "Thanks for your support".plus("\n@" + AuthenticationHelper.currentUsername)
            }
            val yOffset = activity?.resources?.getDimensionPixelSize(R.dimen.marginToast)
            yOffset?.let { offset -> toast?.setGravity(Gravity.TOP, 0, offset) }
            toast?.duration = duration
            toast?.show()
        }
    }

    override fun didTapShowSupports(post: Post) {
        findNavController().navigate(
            R.id.supportsListFragment,
            bundleOf("postId" to post.originalPostId)
        )
    }

    override fun getProfilePicture(
        key: String?,
        ignoringCache: Boolean,
        handler: ImageFetchHandler?
    ) = userProfileImageService.getProfilePicture(key, ignoringCache, handler)


    override fun getCachedUserInfo(userId: Int): UserProfile? {
        return userService.getCachedUserInfo(userId)
    }

    private fun showPostExtraDialog(@LayoutRes layoutRes: Int): BottomSheetDialog {
        val sheetView = activity?.layoutInflater?.inflate(layoutRes, null)
        val actionSheet = BottomSheetDialog(context as Context, R.style.ProfileSheetDialog)
        actionSheet.setContentView(sheetView as View)
        actionSheet.show()
        actionSheet.setOnDismissListener {
            lastBottomDialog = null
        }

        sheetView.findViewById<View>(R.id.dialog_cancel_button)?.setOnClickListener {
            actionSheet.dismiss()
        }
        lastBottomDialog = actionSheet
        return actionSheet
    }

    private fun showPostExtraDialog(post: Post, blocked: Boolean = false) {
        val actionSheet = showPostExtraDialog(R.layout.dialog_bottom_other_post_more_actions)

        post.groupId?.let {
            if (memberShipRole?.getRole(AuthenticationHelper.currentUserId ?: 0) == Role.OWNER
                || memberShipRole?.getRole(AuthenticationHelper.currentUserId ?: 0) == Role.ADMIN
            ) {
                actionSheet.findViewById<View>(R.id.panelDelete)?.show()
                actionSheet.findViewById<View>(R.id.dialog_block_button)
                    ?.setBackgroundResource(R.drawable.profile_actions_item_bg)
            } else {
                actionSheet.findViewById<View>(R.id.panelDelete)?.hide()
                actionSheet.findViewById<View>(R.id.dialog_block_button)
                    ?.setBackgroundResource(R.drawable.profile_actions_bottom_rounded_corner)
            }
        } ?: kotlin.run {
            actionSheet.findViewById<View>(R.id.panelDelete)?.hide()
            actionSheet.findViewById<View>(R.id.dialog_block_button)
                ?.setBackgroundResource(R.drawable.profile_actions_bottom_rounded_corner)
        }

        actionSheet.findViewById<View>(R.id.dialog_report_button)?.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val reasons = resources.getStringArray(R.array.report_reasons_array)
            builder.setTitle("Reason")
            builder.setNegativeButton(getString(R.string.cancel)) { _, _ ->
                actionSheet.dismiss()
            }
            builder.setItems(reasons) { _, which ->
                when (which) {
                    reasons.lastIndex -> {
                        var firstName = ""
                        var secondName = ""
                        userService.getCachedUserInfo(AuthenticationHelper.currentUserId ?: 0)
                            ?.let { user ->
                                firstName = user.firstname
                                secondName = user.lastname
                            }

                        val url = COPYRIGHT_URL_FORMAT.format(post.id, firstName, secondName)
                        findNavController().navigate(
                            R.id.copyrightInfringement,
                            bundleOf("url" to url)
                        )
                    }
                    else -> viewModel.report(post, which + 1)
                }
            }

            builder.create().show()

            actionSheet.dismiss()
        }
        actionSheet.findViewById<TextView>(R.id.dialog_block_text)?.text =
            if (blocked) getString(R.string.unblock_user) else getString(R.string.block_user)
        actionSheet.findViewById<View>(R.id.dialog_block_button)?.setOnClickListener {
            if (blocked) {
                viewModel.unblock(post.user_id).observe(viewLifecycleOwner, Observer {
                    handleLoadState(it) { msg ->
                        snack(msg)
                    }
                })
            } else {
                viewModel.block(post.user_id).observe(viewLifecycleOwner, Observer {
                    handleLoadState(it) { msg ->
                        snack(msg)
                    }
                })
            }
            actionSheet.dismiss()
        }
        actionSheet.findViewById<View>(R.id.dialog_delete_post_button)?.setOnClickListener {
            showConfirmDeleteAlert(post)
            actionSheet.dismiss()
        }
    }

    private fun showUserPostExtraDialog(post: Post) {
        val actionSheet = showPostExtraDialog(R.layout.dialog_bottom_post_more_actions)

        actionSheet.findViewById<View>(R.id.dialog_edit_button)?.setOnClickListener {
            Navigation.findNavController(view as View).navigate(
                R.id.editPostFragment,
                bundleOf("postId" to post.id)
            )
            actionSheet.dismiss()
        }

        actionSheet.findViewById<View>(R.id.dialog_delete_button)?.setOnClickListener {
            showConfirmDeleteAlert(post)
            actionSheet.dismiss()
        }
    }

    private fun showSharedExtraDialog(post: Post) {
        val actionSheet = showPostExtraDialog(R.layout.dialog_bottom_shared_post_more_actions)

        actionSheet.findViewById<View>(R.id.dialog_unshare_button)?.setOnClickListener {
            viewModel.unshare(post)
            actionSheet.dismiss()
        }
    }

    private fun showConfirmDeleteAlert(post: Post) {
        val builder = AlertDialog.Builder(context as Context)
        builder.setTitle(getString(R.string.delete))
        builder.setMessage(getString(R.string.areYouSureDeletePost))

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            viewModel.delete(post.id)
            progressCommunity?.show()
            if (sharedPrefManager.getExclusivePostTime().isNullOrEmpty()
                    .not() && post.privacy == Post.PRIVACY_EXCLUSIVE
            ) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                sharedPrefManager.getExclusivePostTime()?.let { tomorrowAsString ->
                    val todayAsString = dateFormat.format(Calendar.getInstance().time)
                    if (tomorrowAsString.equals(todayAsString, true)) {
                        sharedPrefManager.setExclusivePostTime("")
                    }
                }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    open fun <T> handleLoadState(loadState: Data<T>, onSuccess: (data: T) -> Unit) {
        when (loadState) {
            is Data.Success -> {
                onSuccess.invoke(loadState.data)
            }
            is Data.Error -> {
                loadState.throwable.message?.let { snack(it) }
            }
        }
    }

    fun handleInitialLoadState(loadState: Data<Boolean>) {
        when (loadState) {
            is Data.Success -> {
                post_load_progress_bar?.hide()
            }
            is Data.Loading -> {
                post_load_progress_bar?.show()
            }
            is Data.Error -> {
                post_load_progress_bar?.hide()
                loadState.throwable.message?.let { snack(it) }
            }
        }
    }

    override fun didApprove(post: PendingPost) {}

    override fun didDecline(post: PendingPost) {}

    override fun refreshPosts() {}

    override val lifecycleOwner: LifecycleOwner
        get() = viewLifecycleOwner

    override fun didTapOnVideo(composedText: String) {}

    override fun didTapOnPost(text: String) {}

    override fun didTapOnCamera(composedText: String) {}

    override fun didTapPost() {}

    override fun getAdContent(adapterPosition: Int): AdContent? {
        return postsAdapter.getAdContent(adapterPosition)
    }

    override fun didTapTagUser(userTag: String) {
        findNavController().showUserProfile(userTag)
    }

    override fun didTagUser(tagUser: String) {
        findNavController().showUserProfile(tagUser)
    }

    override fun openMentionSearchFragment(composedText: String, position: Int) {
        //TODO("Not yet implemented")
    }

    override fun didTapHashtag(hashtag: String) {
        findNavController().navigate(
            R.id.hashtagGridFragment, bundleOf("hashtag" to hashtag)
        )
    }

}