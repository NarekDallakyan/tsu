package social.tsu.android.ui.user_profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import kotlinx.android.synthetic.main.user_feed.*
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.UserVideosAdapter
import social.tsu.android.adapters.UserVideosAdapterActionCallback
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.api.UserApi
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.post_feed.user_feed.UserVideosFeedFragmentDirections
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.userProfile.DefaultUserVideosViewModel
import social.tsu.android.viewModel.userProfile.UserVideosViewModel
import social.tsu.android.viewModel.userProfile.UserVideosViewModelCallback
import javax.inject.Inject
import social.tsu.android.utils.hide
import social.tsu.android.utils.show

class UserVideosFragment : Fragment(),
    UserVideosViewModelCallback,
    UserVideosAdapterActionCallback {

    private lateinit var recyclerView: RecyclerView

    private var userId: Int = UserProfile.NO_USER_ID

    private val profileViewModel by viewModels<UserProfileViewModel>(
        ownerProducer = { requireParentFragment() }
    )

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: UserApi

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val viewModel: UserVideosViewModel by lazy {
        DefaultUserVideosViewModel(
            activity?.application as TsuApplication,
            this
        )
    }

    val userVideosAdapter: UserVideosAdapter by lazy {
        UserVideosAdapter(
            activity?.application as TsuApplication,
            this
        )
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

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        val viewManager = GridLayoutManager(activity, 3, GridLayoutManager.VERTICAL, false)

        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            layoutManager = viewManager
            sharedPrefManager.getSupportPostId()?.let { ids ->
                userVideosAdapter.getSupportPostId = ids
            }
            adapter = userVideosAdapter
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private val visibleThreshold = 6
            private var lastVisibleItem = 0
            var totalItemCount = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                totalItemCount = viewManager.itemCount
                lastVisibleItem = viewManager.findLastVisibleItemPosition()
                if (!userVideosAdapter.isLoading && viewModel.nextPage != null
                    && totalItemCount <= (lastVisibleItem + visibleThreshold)
                ) {
                    if (viewModel.nextPage != null) {
                        Log.d("VIREOSFEED", "asking for page ${viewModel.nextPage}")
                        userVideosAdapter.isLoading = true
                        userVideosAdapter.notifyDataSetChanged()
                        fetchData(viewModel.nextPage)
                    } else {
                        Log.d("VIREOSFEED", "Not triggering loadmore as next_page is null")
                    }

                }
            }
        })

        profileViewModel.userProfileLiveData.observe(viewLifecycleOwner, Observer {
            this.userId = it.id
            if (viewModel.nextPage == 0) {
                fetchData(null)
            } else if (viewModel.nextPage != null) {
                fetchData(viewModel.nextPage)
            }
            viewModel.videosForUser(it.id).observe(viewLifecycleOwner, Observer { items ->
                userVideosAdapter.updatePosts(items)
            })
        })

        return view
    }

    private fun fetchData(nextPage: Int?) {
        viewModel.loadUserVideos(userId, nextPage)
    }

    override fun didUpdateVideos(nextPage: Int?) {
        userVideosAdapter.isLoading = false
    }

    override fun didErrorWith(message: String) {
        snack(message)
    }

    override fun onVideoClicked(post: Post, position: Int) {
        Log.d("UserVideosFragment", "show post id ${post.id}")
        val action = UserVideosFeedFragmentDirections.showUserVideosFeedFragment()
        action.postId = post.id
        action.postPosition = position
        action.userId = userId
        findNavController().navigate(action)
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