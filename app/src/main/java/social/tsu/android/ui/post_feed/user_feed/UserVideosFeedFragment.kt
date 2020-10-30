package social.tsu.android.ui.post_feed.user_feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.android.support.AndroidSupportInjection
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.ads.GoogleAdFetcher
import social.tsu.android.ui.post_feed.BaseFeedFragment
import social.tsu.android.ui.post_feed.UserPostsAdapter
import social.tsu.android.ui.util.RetryCallback
import javax.inject.Inject


private const val TAG = "UserVideosFeedFragment"

class UserVideosFeedFragment : BaseFeedFragment<UserFeedViewModel>(), RetryCallback {

    private lateinit var recyclerView: RecyclerView

    private var userId: Int = 0

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var tsuApplication: TsuApplication

    override val postsAdapter by lazy {
        UserPostsAdapter(
            tsuApplication,
            exoPlayer,
            actionCallback = this,
            retryCallback = this,
            addPostComposeView = false,
            nativeAdFetcher = GoogleAdFetcher.getInstance(
                "VideosFeed",
                requireActivity()
            )
        )
    }

    override val viewModel by viewModels<UserFeedViewModel> { viewModelFactory }

    private val args by navArgs<UserVideosFeedFragmentArgs>()

    private var initialScrollCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(
            R.layout.user_feed,
            container, false
        )

        if (userId == 0) {
            if (args.userId > 0 ) {
                userId = args.userId
            } else {
                AuthenticationHelper.currentUserId?.let {
                    userId = it
                } ?: run {
                    Log.e(TAG, "No Valid UserId provided!")
                }
            }
        }

        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            layoutManager = postsLayoutManager
            adapter = postsAdapter
        }
        recyclerView.setHasFixedSize(false)

        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        viewModel.getUserVideoPosts(userId, args.postPosition)
            .observe(viewLifecycleOwner, Observer {
                postsAdapter.submitList(it) {
                    if (!initialScrollCompleted && it.size >= args.postPosition) {
                        initialScrollCompleted = true
                        lifecycleScope.launchWhenResumed {
                            recyclerView.scrollToPosition(args.postPosition)
                            postsAdapter.playVideo(args.postPosition, args.postId)
                        }
                    }
                }
            })

        return view
    }

    override fun didTapOnUser(userId: Long) {
        if (this.userId.toLong() != userId) super.didTapOnUser(userId)
    }

    override fun refreshPosts() {
        viewModel.refreshUserVideos(userId)
    }

    override fun retry() {
        viewModel.retry()
    }

}