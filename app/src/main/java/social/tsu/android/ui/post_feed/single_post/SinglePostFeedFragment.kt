package social.tsu.android.ui.post_feed.single_post


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import dagger.android.support.AndroidSupportInjection
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.post_feed.BaseFeedFragment
import social.tsu.android.ui.post_feed.UserPostsAdapter
import social.tsu.android.ui.util.RetryCallback
import social.tsu.android.utils.snack
import javax.inject.Inject

class SinglePostFeedFragment : BaseFeedFragment<SinglePostFeedViewModel>(), RetryCallback {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val viewModel by viewModels<SinglePostFeedViewModel> { viewModelFactory }

    private val args by navArgs<SinglePostFeedFragmentArgs>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var progressbar: ProgressBar?=null
    override val postsAdapter by lazy {
        UserPostsAdapter(
            activity?.application as TsuApplication,
            exoPlayer,
            actionCallback = this,
            retryCallback = this,
            addPostComposeView = false
        )
    }

    private val dataSetObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            findNavController().popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(
            R.layout.fragment_single_post_feed,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = view.findViewById(R.id.main_feed_swipe_to_refresh)
        swipeRefreshLayout.setOnRefreshListener { retry() }
        progressbar = view.findViewById(R.id.post_load_progress_bar) as ProgressBar
        progressbar?.visibility = View.VISIBLE
        recyclerView = view.findViewById<RecyclerView>(R.id.posts).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postsAdapter
        }

        if (args.postId > 0) {
            viewModel.postId = args.postId
            viewModel.loadState.observe(viewLifecycleOwner, Observer {
                when (it) {
                    is Data.Success -> {
                        swipeRefreshLayout.isRefreshing = false
                        progressbar?.visibility = View.GONE
                    }
                    is Data.Loading -> {
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is Data.Error -> {
                        swipeRefreshLayout.isRefreshing = false
                        it.throwable.message?.let { message -> snack(message) }
                    }
                }
            })

            viewModel.post.observe(viewLifecycleOwner, Observer(postsAdapter::submitList))

            postsAdapter.registerAdapterDataObserver(dataSetObserver)
        }

    }

    override fun retry() {
        viewModel.retry()
    }

    override fun onDestroyView() {
        postsAdapter.unregisterAdapterDataObserver(dataSetObserver)
        super.onDestroyView()
    }

    override fun openMentionSearchFragment(composedText: String,position: Int) {
        TODO("Not yet implemented")
    }

}
