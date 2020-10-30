package social.tsu.android.ui.hashtag

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.HashtagGridAdapter
import social.tsu.android.data.local.entity.Post
import social.tsu.android.ui.PostGridActionCallback
import social.tsu.android.ui.hashtag.feed.HashtagFeedFragmentDirections
import social.tsu.android.ui.recyclerview.SpanSize
import social.tsu.android.ui.recyclerview.SpannedGridLayoutManager
import social.tsu.android.utils.snack

class HashtagGridFragment : Fragment(), HashtagGridViewModelCallback, PostGridActionCallback {

    private val args by navArgs<HashtagGridFragmentArgs>()

    private lateinit var recyclerView: RecyclerView

    var lastItemVisiblePosition: Int? = null
    var islastPositionUpdated: Boolean = false

    private var hashtagPosition: Int? = null

    val viewModel: HashtagGridViewModel by lazy {
        DefaultHashtagGridViewModel(activity?.application as TsuApplication, this)
    }

    val hashtagGridAdapter: HashtagGridAdapter by lazy {
        HashtagGridAdapter( this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hashtag_grid, container, false)

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
            adapter = hashtagGridAdapter

            val spacing = resources.getDimensionPixelSize(R.dimen.grid_item_spacing)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    outRect.left = spacing
                    outRect.top = spacing
                    outRect.right = spacing
                    outRect.bottom = spacing
                }
            })
        }

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            private val visibleThreshold = 5
            private var lastVisibleItem = 0
            var totalItemCount = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!islastPositionUpdated) {
                    lastItemVisiblePosition = viewManager.lastVisiblePosition
                    Log.d("HashtagGridFragment", "last visible $lastItemVisiblePosition")
                    islastPositionUpdated = true
                }

                totalItemCount = viewManager.itemCount
                lastVisibleItem = viewManager.lastVisiblePosition
                if (!hashtagGridAdapter.isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if(viewModel.nextPage != null){
                        Log.d("HashtagGridFragment", "asking for page ${viewModel.nextPage}")
                        hashtagGridAdapter.isLoading = true
                        hashtagGridAdapter.notifyDataSetChanged()
                        fetchData(viewModel.nextPage)
                    } else {
                        Log.d("HashtagGridFragment", "Not triggering loadmore as next_page is null")
                    }
                }
            }
        })

            if (viewModel.nextPage == 0) {
                fetchData(null)
            } else if (viewModel.nextPage != null) {
                fetchData(viewModel.nextPage)
            }
            viewModel.getCacheByHashtag(args.hashtag ?: "").observe(viewLifecycleOwner, Observer { items ->
                hashtagGridAdapter.updatePosts(items)
            })

        return view
    }

    private fun fetchData(nextPage: Int?) {
        viewModel.getPostsByHashtag(args.hashtag ?: "", nextPage)
    }

    override fun didUpdateHashtags(nextPage: Int?) {
        hashtagGridAdapter.isLoading = false
    }

    override fun didErrorWith(message: String) {
        snack(message)
    }

    override fun onPostClicked(post: Post, position: Int) {
        Log.d("HashtagGridFragment", "show post id ${post.id}")
        hashtagPosition = position
        val action = HashtagFeedFragmentDirections
            .openHashtagFeedFragment()
            .apply {
                hashtag = args.hashtag ?: ""
                postPosition = position
            }
        findNavController().navigate(action)
    }

    override fun onStart() {
        hashtagPositionChange()
        super.onStart()
    }

    override fun onResume() {
        hashtagPositionChange()
        super.onResume()
    }

    private fun hashtagPositionChange() {
        if (hashtagPosition != null) {
            recyclerView.layoutManager?.scrollToPosition(hashtagPosition!!)
            hashtagPosition = null
        }
    }
}