package social.tsu.android.ui.post_feed.user_feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.android.support.AndroidSupportInjection
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.post_feed.BaseFeedFragment
import social.tsu.android.ui.post_feed.UserPostsAdapter
import social.tsu.android.ui.user_profile.UserProfileViewModel
import social.tsu.android.ui.util.RetryCallback
import javax.inject.Inject
import kotlinx.android.synthetic.main.user_feed.*
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import androidx.navigation.fragment.findNavController
import social.tsu.android.helper.ads.GoogleAdFetcher

class UserFeedFragment: BaseFeedFragment<UserFeedViewModel>(), RetryCallback {

    override val lifecycleOwner: LifecycleOwner
        get() = viewLifecycleOwner

    private val profileViewModel by viewModels<UserProfileViewModel>(
        ownerProducer = { requireParentFragment() }
    )

    private lateinit var recyclerView: RecyclerView

    private var userId: Int = UserProfile.NO_USER_ID
    private var blocked: Boolean = false

    @Inject
    lateinit var schedulers: RxSchedulers

    override val postsAdapter by lazy {
        UserPostsAdapter(
            activity?.application as TsuApplication,
            exoPlayer,
            actionCallback = this,
            retryCallback = this,
            addPostComposeView = false,
            nativeAdFetcher = GoogleAdFetcher.getInstance(
                "UserFeed",
                requireActivity()
            )
           // dividerColor = args.dividerColor
        )
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val viewModel by viewModels<UserFeedViewModel> { viewModelFactory }

    private val args by navArgs<UserFeedFragmentArgs>()

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

        if (userId < 0) {
            userId = args.userId.toInt()

            if (userId < 0) {
                AuthenticationHelper.currentUserId?.let {
                    userId = it
                } ?: run {
                    Log.e("UserFeedFragment", "No Valid UserId provided!")
                }
            }
        }

        profileViewModel.userProfileLiveData.observe(viewLifecycleOwner, Observer {
            this.userId = it.id
            viewModel.getUserPosts(userId)
                .observe(viewLifecycleOwner, Observer(postsAdapter::submitList))
        })

        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            layoutManager = postsLayoutManager
            adapter = postsAdapter
            setHasFixedSize(false)
        }

        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        viewModel.postsLoadState.observe(
            viewLifecycleOwner,
            Observer(postsAdapter::setLoadState)
        )
        profileViewModel.userProfileLiveData.observe(viewLifecycleOwner, Observer { userProfile ->
            userProfile?.let { blocked = it.isBlocked }
        })

        return view
    }

    override fun didTapOnUser(userId: Long) {
        if (profileViewModel.userId.toLong() != userId) super.didTapOnUser(userId)
    }

    override fun openMentionSearchFragment(composedText: String,position: Int) {
        TODO("Not yet implemented")
    }


    override fun retry() {
        viewModel.retry()
    }

    override fun refreshPosts() {
        // disable first recycler item refreshing user feed on bind
        //viewModel.refreshUserPosts(userId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileViewModel.isBlock.observe(viewLifecycleOwner, Observer {
            if (it) {
                posts?.hide()
            } else
                posts?.show()

        })
    }

}




