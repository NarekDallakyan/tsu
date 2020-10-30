package social.tsu.android.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import social.tsu.android.LegacyUserPostsAdapter
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.UserPostsAdapterActionCallback
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.PendingPost
import social.tsu.android.ui.LegacyBaseFeedFragment
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.utils.createAppExoPlayer
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.viewModel.community.CommunityPublishingRequestsCallback
import social.tsu.android.viewModel.community.CommunityPublishingRequestsViewModel
import social.tsu.android.viewModel.community.DefaultCommunityPublishingRequestsViewModel

class CommunityPublishingRequestsFragment :
    LegacyBaseFeedFragment<CommunityPublishingRequestsViewModel>(), UserPostsAdapterActionCallback,
    CommunityPublishingRequestsCallback {

    private val args by navArgs<CommunityPublishingRequestsFragmentArgs>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var errorTextView: TextView
    private lateinit var progressBar: ProgressBar

    private val exoPlayer: SimpleExoPlayer by lazy {
        requireActivity().createAppExoPlayer()
    }

    override val postsAdapter: LegacyUserPostsAdapter by lazy {
        LegacyUserPostsAdapter(
            activity?.application as TsuApplication,
            exoPlayer,
            this,
            null,
            addPostComposeView = true,
            showCommunityInTitle = false
        )
    }

    override val viewModel: CommunityPublishingRequestsViewModel by lazy {
        DefaultCommunityPublishingRequestsViewModel(activity?.application as TsuApplication, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_community_publishing_requests, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        errorTextView = view.findViewById(R.id.errorTextView)
        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = postsAdapter

        if (args.groupId != 0) {
            if (requireActivity().isInternetAvailable())
                viewModel.getPendingPosts(args.groupId)
            else
                requireActivity().internetSnack()
        } else
            showHidePendingPosts()
        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        return view
    }

    private fun showHidePendingPosts() {
        if (postsAdapter.content.isEmpty()) {
            errorTextView.show()
            recyclerView.hide()
        } else {
            errorTextView.hide()
            recyclerView.show()
        }
        progressBar.hide()
    }

    override fun didTapOnCamera(composedText: String) {}

    override fun didTapOnVideo(composedText: String) {}

    override fun didTapOnPost() {}

    override fun didTapLink(link: String) {}

    override fun didTapLikeOn(post: Post) {}

    override fun didTapCommentOn(post: Post) {}

    override fun didTapShowLikes(post: Post) {}

    override fun didTapOnUser(userId: Long) {}

    override fun didTapOnGroup(groupId: Int) {}

    override fun didTapOnShare(post: Post) {}

    override fun didTapMoreOptions(post: Post) {}

    override fun didApprove(post: PendingPost) {
        if (requireActivity().isInternetAvailable()) {
            viewModel.approve(post, args.groupId)
            progressBar.show()
        } else
            requireActivity().internetSnack()
    }

    override fun didDecline(post: PendingPost) {
        if (requireActivity().isInternetAvailable()) {
            viewModel.decline(post, args.groupId)
            progressBar.show()
        } else
            requireActivity().internetSnack()
    }

    override fun onUserClick(id: Int?) {
        findNavController().showUserProfile(id)
    }

    override fun didLoadPendingPosts(posts: List<PendingPost>) {
        postsAdapter.updatePendingPosts(posts)
        showHidePendingPosts()
    }

    override fun didApprovePost(post: PendingPost) {
        postsAdapter.removePost(post)
        showHidePendingPosts()
    }

    override fun didFailApprovePost(post: PendingPost) {
        progressBar.hide()
    }

    override fun didDeclinePost(post: PendingPost) {
        postsAdapter.removePost(post)
        showHidePendingPosts()
    }

    override fun didFailDeclinePost(post: PendingPost) {
        progressBar.hide()
    }

    override fun didFailedToLoad(message: String) {
        progressBar.hide()
    }

    override fun didTapHashtag(hashtag: String) {
        findNavController().navigate(
            R.id.hashtagGridFragment, bundleOf(
                "hashtag" to hashtag
            )
        )
    }

    override fun didTapTagUser(tagUser: String) {
        findNavController().showUserProfile(tagUser)
    }
}