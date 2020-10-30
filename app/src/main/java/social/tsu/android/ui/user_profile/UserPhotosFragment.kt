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
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.user_feed.*
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.UserPhotosAdapter
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.api.UserApi
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.ui.PostGridActionCallback
import social.tsu.android.ui.post_feed.user_feed.UserPhotosFeedFragmentDirections
import social.tsu.android.ui.recyclerview.SpanSize
import social.tsu.android.ui.recyclerview.SpannedGridLayoutManager
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.userProfile.DefaultUserPhotosViewModel
import social.tsu.android.viewModel.userProfile.UserPhotosViewModel
import social.tsu.android.viewModel.userProfile.UserPhotosViewModelCallback
import javax.inject.Inject

class UserPhotosFragment : Fragment(),
    UserPhotosViewModelCallback,
    PostGridActionCallback {

    private lateinit var recyclerView: RecyclerView

    private val profileViewModel by viewModels<UserProfileViewModel>(
        ownerProducer = { requireParentFragment() }
    )

    var lastItemVisiblePosition: Int? = null
    var islastPositionUpdated: Boolean = false

    private var userId: Int = UserProfile.NO_USER_ID

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: UserApi

    private var photoPosition: Int? = null

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val viewModel: UserPhotosViewModel by lazy {
        DefaultUserPhotosViewModel(
            activity?.application as TsuApplication,
            this
        )
    }

    val photosAdapter: UserPhotosAdapter by lazy {
        UserPhotosAdapter(
            activity?.application as TsuApplication,
            this)
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

        val viewManager = SpannedGridLayoutManager(SpannedGridLayoutManager.Orientation.VERTICAL, 3)

        viewManager.spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup { position ->
            when {
                position in 0..3  ->
                    SpanSize(1, 1)
                position % 4 == 0 && (position / 3) % 4 == 1 ->
                    SpanSize(2, 2)
                position % 4 == 1 && (position / 3) % 4 == 3 ->
                    SpanSize(2, 2)
                else ->
                    SpanSize(1, 1)
            }
        }

        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            layoutManager = viewManager
            sharedPrefManager.getSupportPostId()?.let { ids ->
                photosAdapter.getSupportPostId = ids
            }
            adapter = photosAdapter
        }

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener(){

            private val visibleThreshold = 5
            private var lastVisibleItem = 0
            var totalItemCount = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!islastPositionUpdated) {
                    lastItemVisiblePosition = viewManager.lastVisiblePosition
                    Log.d("LAST_VISIBLE", "last visible $lastItemVisiblePosition")
                    islastPositionUpdated = true
                }

                totalItemCount = viewManager.itemCount
                lastVisibleItem = viewManager.lastVisiblePosition
                if (!photosAdapter.isLoading
                    && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if(viewModel.nextPage != null){
                        Log.d("PHOTOSFEED", "asking for page ${viewModel.nextPage}")
                        photosAdapter.isLoading = true
                        photosAdapter.notifyDataSetChanged()
                        fetchData(viewModel.nextPage)
                    } else {
                        Log.d("PHOTOSFEED", "Not triggering loadmore as next_page is null")
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
            viewModel.photosForUser(it.id).observe(viewLifecycleOwner, Observer { items ->
                photosAdapter.updatePosts(items)
            })
        })

        return view
    }

    private fun fetchData(nextPage: Int?) {
        viewModel.getUserPhotos(userId, nextPage)
    }

    override fun didUpdatePhotos(nextPage: Int?) {
        photosAdapter.isLoading = false
    }

    override fun didErrorWith(message: String) {
        snack(message)
    }

    override fun onPostClicked(post: Post, position: Int) {
        Log.d("UserPhotosFragment", "show post id ${post.id}")
        val action = UserPhotosFeedFragmentDirections.showUserPhotosFeedFragment()
        photoPosition = position
        action.postId = post.id
        action.postPosition = position
        action.userId = userId
        findNavController().navigate(action)
    }

    override fun onStart() {
        photoPositionChange()
        super.onStart()
    }

    override fun onResume() {
        photoPositionChange()
        super.onResume()
    }

    private fun photoPositionChange() {
        if (photoPosition != null) {
            if (photoPosition!! > lastItemVisiblePosition!!) {
                collapseToolbar()
            }
            recyclerView.layoutManager?.scrollToPosition(photoPosition!!)
            photoPosition = null
        }
    }

    private fun collapseToolbar() {
        Log.d("PROFILE_PHOTO", "profile hide")
        var parentFrag: UserProfileFragment =
            this@UserPhotosFragment.parentFragment as UserProfileFragment
        parentFrag.collapseToolbar()
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