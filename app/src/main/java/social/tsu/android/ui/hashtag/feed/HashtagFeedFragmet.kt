package social.tsu.android.ui.hashtag.feed

import android.os.Bundle
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
import social.tsu.android.helper.ads.GoogleAdFetcher
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.post_feed.BaseFeedFragment
import social.tsu.android.ui.post_feed.UserPostsAdapter
import social.tsu.android.ui.util.RetryCallback
import javax.inject.Inject

class HashtagFeedFragment : BaseFeedFragment<HashtagFeedViewModel>(), RetryCallback {

    private lateinit var recyclerView: RecyclerView

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val postsAdapter by lazy {
        UserPostsAdapter(
            activity?.application as TsuApplication,
            exoPlayer,
            actionCallback = this,
            retryCallback = this,
            addPostComposeView = false,
            nativeAdFetcher = GoogleAdFetcher.getInstance(
                "HashtagFeed",
                requireActivity()
            )
        )
    }

    override val viewModel by viewModels<HashtagFeedViewModel> { viewModelFactory }

    private val args by navArgs<HashtagFeedFragmentArgs>()

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

        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            layoutManager = postsLayoutManager
            adapter = postsAdapter
        }
        recyclerView.setHasFixedSize(false)

        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        viewModel.getHashtagPosts(args.hashtag, args.postPosition)
            .observe(viewLifecycleOwner, Observer {
                postsAdapter.submitList(it)
                if (!initialScrollCompleted && it.size >= args.postPosition) {
                    initialScrollCompleted = true
                    lifecycleScope.launchWhenResumed {
                        recyclerView.scrollToPosition(args.postPosition)
                    }
                }
            })

        return view
    }

    override fun refreshPosts() {
        if (requireActivity().isInternetAvailable())
            viewModel.refreshHashtagPosts(args.hashtag)
        else
            requireActivity().internetSnack()
    }

    override fun retry() {
        viewModel.retry()
    }
}