package social.tsu.android.ui.post_feed.main

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indicative.client.android.Indicative
import kotlinx.android.synthetic.main.compose_post.*
import kotlinx.android.synthetic.main.feed.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.ads.GoogleAdFetcher
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.MainActivity.Companion.NEW_POST_FROM_FEED_PERMISSIONS_REQUEST_CODE
import social.tsu.android.ui.checkPermissions
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.post_feed.BaseFeedFragment
import social.tsu.android.ui.post_feed.UserPostsAdapter
import social.tsu.android.ui.search.MENTION_TYPE
import social.tsu.android.ui.search.SearchFragment
import social.tsu.android.ui.util.RetryCallback
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.MentionViewModel
import social.tsu.android.viewModel.SharedViewModel
import javax.inject.Inject


class MainFeedFragment : BaseFeedFragment<MainFeedViewModel>(), RetryCallback {

    private var isFeedTypeTrending: Boolean = false
    private lateinit var feedTypeTitle: TextView
    private lateinit var feedTypeAppBar: AppBarLayout

    override val lifecycleOwner: LifecycleOwner
        get() = viewLifecycleOwner

    private lateinit var recyclerView: RecyclerView

    private val mainActivity: MainActivity?
        get() = activity as? MainActivity

    var position = 0


    @IdRes
    var defaultSelection: Int = R.id.photoCaptureFragment2
    private var postText: String = ""
    override val postsAdapter by lazy {
        UserPostsAdapter(
            activity?.application as TsuApplication,
            exoPlayer,
            actionCallback = this,
            retryCallback = this,
            nativeAdFetcher = GoogleAdFetcher.getInstance(
                "MainFeed",
                requireActivity()
            )
        )
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    override val viewModel by viewModels<MainFeedViewModel> { viewModelFactory }

    private var model: SharedViewModel? = null
    private var mentionViewModel: MentionViewModel? = null

    override fun refreshPosts() {
        if (requireActivity().isInternetAvailable()) {
            if (!isFeedTypeTrending) {
                viewModel.refreshPosts()
            }
        } else {
            requireActivity().internetSnack()
        }
    }

    var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthenticationHelper.authToken != null) {
            when (sharedPrefManager.getFeedType() ?: SharedPrefManager.MAIN_FEED_TYPE_CHRONO) {
                SharedPrefManager.MAIN_FEED_TYPE_CHRONO -> {
                    isFeedTypeTrending = false

                    viewModel.getMainFeedChrono().observe(this, Observer {
                        postsAdapter.submitList(it)
                        if (it.size > 0) {
                            progressBar?.visibility = View.GONE
                        }
                    })
                }
                SharedPrefManager.MAIN_FEED_TYPE_TREND -> {
                    isFeedTypeTrending = true
                    viewModel.getMainFeedTrending(true).observe(this, Observer {
                        postsAdapter.submitList(it)
                        if (it.size > 0) {
                            progressBar?.visibility = View.GONE
                        }
                    })
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("TSUVIEW", "MainFeedFragment onCreateView")
        val view = inflater.inflate(
            R.layout.feed,
            container, false
        )

        feedTypeTitle = view.findViewById(R.id.feed_type_text_view)
        feedTypeAppBar = view.findViewById(R.id.feed_toggle)

        when (isFeedTypeTrending) {
            true -> feedTypeTitle.setText(R.string.feed_type_trending_beta)
            false -> feedTypeTitle.setText(R.string.feed_type_chronological)
        }

        view.findViewById<ImageButton>(R.id.feed_settings_button).setOnClickListener {
            findNavController().navigate(R.id.feedSettingsFragment)
        }

        progressBar = view.findViewById(R.id.post_load_progress_bar) as ProgressBar

        progressBar?.visibility = View.VISIBLE
        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            if (layoutManager == null) layoutManager = postsLayoutManager
            adapter = postsAdapter
        }

        model = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        mentionViewModel = ViewModelProvider(requireActivity()).get(MentionViewModel::class.java)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        if (AuthenticationHelper.authToken != null) {

            viewModel.initialLoadState.observe(viewLifecycleOwner, Observer {
                handleInitialLoadState(it)
            })

            viewModel.userRefreshLoadState.observe(viewLifecycleOwner, Observer {
                main_feed_swipe_to_refresh.isRefreshing = it is Data.Loading
                handleLoadState(it) {}
            })

            if (requireActivity().isInternetAvailable()) {
                if (!isFeedTypeTrending) {
                    viewModel.refreshPosts()
                }
            } else {
                requireActivity().internetSnack()
            }
            viewModel.postsLoadState.observe(
                viewLifecycleOwner,
                Observer(postsAdapter::setLoadState)
            )

        } else
            findNavController().navigate(R.id.showLoginFragment)

        setupUI(view)
        Log.d("TSUVIEW", "MainFeedFragment onCreateView done")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("TSUVIEW", "MainFeedFragment onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        Log.d("TSUVIEW", "MainFeedFragment onViewCreated done")
    }

    override fun openMentionSearchFragment(composedText: String, position: Int) {
        dismissKeyboard()
        this.position = position
        Log.e("position", "position : $position")
        postText = composedText

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.mainFeedFragment) {
            navController.navigate(
                R.id.action_mainFeedFragment_to_searchFragment,
                bundleOf(
                    "searchType" to SearchFragment.SEARCH_TYPE_MENTION,
                    MENTION_TYPE to SearchFragment.MENTION_TYPE_MAINPOST
                )
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {

        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                dismissKeyboard()
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        model!!.getSelected().observe(viewLifecycleOwner, Observer {
            if (it) {
                postText = ""
                postsAdapter.updateComposetext(postText)
                postsAdapter.notifyDataSetChanged()
            } else {
                if (mentionViewModel?.selected != null) {
                    val first: String = postText.substring(0, position)
                    val second: String = postText.substring(position)
                    val finalText = first.plus(mentionViewModel?.selected).plus(second)
                    Log.d("MAIN_VIEW_MODEL", "final text $finalText ")
                    postsAdapter.updateComposetext(finalText)
                    postsAdapter.notifyDataSetChanged()
                    mentionViewModel?.selected = null
                    postText = ""
                } else {
                    postsAdapter.updateComposetext(postText)
                    postsAdapter.notifyDataSetChanged()
                }
            }
        })
        mainActivity?.invalidateOptionsMenu()
        mainActivity?.findViewById<View>(R.id.feed_toolbar_buttons)?.show()
        mainActivity?.setupTopLevelConfigurations(R.id.mainFeedFragment)
        mainActivity?.updateNotificationsCount()
        Log.d("setuserid:", " it is:" + AuthenticationHelper.authToken)
        Indicative.setUniqueID(AuthenticationHelper.authToken)
        AuthenticationHelper.currentUserId?.let {
            Log.d("setuserid: userId", " it is:" + it)
            sharedPrefManager.setUserId(it)
        }
//        mainActivity?.loadCurrentUser()

        main_feed_swipe_to_refresh?.setOnRefreshListener {
            if (requireActivity().isInternetAvailable())
                viewModel.refreshPosts(true, isFeedTypeTrending)
            else
                requireActivity().internetSnack()
        }
        main_feed_swipe_to_refresh?.measure(
            ViewPager.LayoutParams.MATCH_PARENT,
            ViewPager.LayoutParams.WRAP_CONTENT
        )
        (activity as? MainActivity)?.findViewById<View>(R.id.toolbar_logo)?.setOnClickListener {
            recyclerView.scrollToPosition(0)
            feedTypeAppBar.setExpanded(true)
        }
        mainActivity?.findViewById<BottomNavigationView>(R.id.bottom_navbar)?.menu?.findItem(R.id.mainFeedFragment)
            ?.setOnMenuItemClickListener {
                recyclerView.scrollToPosition(0)
                feedTypeAppBar.setExpanded(true)
                return@setOnMenuItemClickListener true
            }
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.show()
    }

    override fun onPause() {
        super.onPause()
        mainActivity?.findViewById<BottomNavigationView>(R.id.bottom_navbar)?.menu?.findItem(R.id.mainFeedFragment)
            ?.setOnMenuItemClickListener {
                return@setOnMenuItemClickListener false
            }
    }

    /**
     * Opens media capture fragment with corresponding media type (photo|video) selected
     * and preserving current destination for navigation, defaulting to main feed if unavailable
     */
    fun openPostTypesFragment() {
        dismissKeyboard()
        val navController = findNavController()
        navController.navigate(
            MainFeedFragmentDirections.showPostTypesFragment(postText)
                .apply {
                    popToDestination = navController.currentDestination?.id ?: R.id.mainFeedFragment
                    defaultPostType = defaultSelection
                })
    }

    override fun didTapOnCamera(composedText: String) {
        defaultSelection = R.id.photoCaptureFragment2
        postText = composedText
        checkPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ),
            NEW_POST_FROM_FEED_PERMISSIONS_REQUEST_CODE
        ) {
            openPostTypesFragment()
        }
    }

    /**
     * Callback function from CreatePostViewHolder.ViewHolderActions that is called when user taps
     * on "Add video" button in "Create Post" feed item that appears on top of the user feed
     * @param composedText text that user already wrote in post text field
     */
    override fun didTapOnVideo(composedText: String) {
        defaultSelection = R.id.videoCaptureFragment2
        postText = composedText
        checkPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ),
            NEW_POST_FROM_FEED_PERMISSIONS_REQUEST_CODE
        ) {
            openPostTypesFragment()
        }
    }

    override fun didTapOnPost(text: String) {
        dismissKeyboard()
        if (!text.trim().equals("")) {
            createPost(text)
        } else {
            Toast.makeText(
                context,
                getString(R.string.enter_text_message),
                Toast.LENGTH_LONG
            ).show()

        }

    }

    private fun createPost(textToPost: String) {
        if (textToPost.isNotEmpty()) {
            if (requireActivity().isInternetAvailable()) {

                viewModel.createPost(textToPost, isFeedTypeTrending)
                    .observe(viewLifecycleOwner, Observer {
                        when (it) {
                            is Data.Success -> {
                                composePost.setText("")
                                btn_post.show()
                                progress_bar.hide()
                                dismissKeyboard()
                            }
                            is Data.Loading -> {
                                btn_post.hide()
                                progress_bar.show()
                            }
                            is Data.Error -> {
                                btn_post.show()
                                progress_bar.hide()
                                it.throwable.message?.let { msg -> snack(msg) }
                            }
                        }
                    })
            } else
                requireActivity().internetSnack()
        }
    }

    override fun retry() {
        viewModel.retry()
    }

}


