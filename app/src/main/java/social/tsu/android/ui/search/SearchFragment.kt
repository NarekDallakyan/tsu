package social.tsu.android.ui.search

import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.textfield.TextInputEditText
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.search.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import retrofit2.Response
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.SearchApi
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.DiscoveryGridAdapter
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.*
import social.tsu.android.rx.plusAssign
import social.tsu.android.ui.PostGridActionCallback
import social.tsu.android.ui.community.CommunityViewModel
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.post_feed.community.CommunityFeedFragmentDirections
import social.tsu.android.ui.recyclerview.SpanSize
import social.tsu.android.ui.recyclerview.SpannedGridLayoutManager
import social.tsu.android.utils.*
import social.tsu.android.viewModel.MentionViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject


private const val AUTO_SUGGEST_THROTTLE_INTERVAL = 100L
private val AUTO_SUGGEST_THROTTLE_UNIT = TimeUnit.MILLISECONDS
const val MENTION_TYPE = "mentionType"

class SearchFragment : Fragment(), CoroutineScope by MainScope(), PostGridActionCallback,
    DiscoveryGridViewModelCallback {

    private val DISCOVERY_TAG = "Search Screen Discovery Grid"

    companion object {
        const val SEARCH_TYPE_ANY = 0
        const val SEARCH_TYPE_USERS = 1
        const val SEARCH_TYPE_GROUP_TOPICS = 2
        const val SEARCH_TYPE_GROUPS = 3
        const val SEARCH_TYPE_HASHTAGS = 4
        const val SEARCH_TYPE_MENTION = 5
        const val MENTION_TYPE_MAINPOST = 6
        const val MENTION_TYPE_COMMUNITY = 7
        const val MENTION_TYPE_COMMENT = 8
        const val MENTION_TYPE_CHAT = 9
        const val MENTION_TYPE_EDIT_POST = 10
    }

    private var mToast: Toast? = null
    private val compositeDisposable = CompositeDisposable()
    private val autoCompletePublishSubject = PublishSubject.create<String>()
    private val model: CommunityViewModel by activityViewModels()
    private val mentionViewModel: MentionViewModel by activityViewModels()
    val properties = HashMap<String, Any?>()
    var searchTerm = ""
    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var searchApi: SearchApi

    val args: SearchFragmentArgs by navArgs()

    private var progressBar: ProgressBar? = null

    private val searchResultsAdapter = SearchResultsAdapter(object : OnItemClickListener<Any> {
        override fun onItemClicked(item: View, data: Any) {
            Log.d("CLICK", "You clicked $data")
            dismissKeyboard()
            when (data) {
                is MentionUser -> {
                    if (args.mentionType == MENTION_TYPE_MAINPOST) {
                        mentionViewModel.selected = data.username
                        findNavController().navigateUp()
                    }
                }
                is SearchUser -> {
                    if (args.searchType == SEARCH_TYPE_MENTION) {
                        if (args.mentionType == MENTION_TYPE_MAINPOST){
                            mentionViewModel.selected = data.username
                            properties["userId"] = data.id

                            findNavController().navigateUp()
                        }else if (args.mentionType == MENTION_TYPE_COMMUNITY){
                            Log.d("MENTION_CALLBACK" , "calling here")
                            properties["userId"] = data.id
                            mentionViewModel.selected = data.username
                            findParentNavController().popBackStack(R.id.postDraftFragment, false)
                        }

                    } else {
                        properties["userId"] = data.id
                        Navigation.findNavController(item).showUserProfile(data.id)
                    }
                }
                is Group -> {
                    if (args.searchType == SEARCH_TYPE_GROUP_TOPICS) {
                        model.selectedGroup = data
                        properties["communityId"] = data.ownerId
                        findNavController().navigateUp()
                    } else {
                        val action = CommunityFeedFragmentDirections.openCommunityFeedFragment()
                        action.group = data
                        properties["communityId"] = data.ownerId
                        findNavController().navigate(action, navOptions {
                            anim {
                                enter = R.anim.slide_enter_rtl
                                exit = R.anim.slide_exit_rtl
                                popEnter = R.anim.slide_pop_enter_rtl
                                popExit = R.anim.slide_pop_exit_rtl
                            }
                        })
                    }
                }
                is HashTag -> {
                    properties["userId"] = data.id
                    findNavController().navigate(
                        R.id.hashtagGridFragment,
                        bundleOf("hashtag" to data.text)
                    )
                }
            }
        }
    })

    private lateinit var discoveryRecyclerView: RecyclerView

    var lastItemVisiblePosition: Int? = null
    var islastPositionUpdated: Boolean = false

    private var postPosition: Int? = null

    val discoveryGridViewModel: DiscoveryGridViewModel by lazy {
        DefaultDiscoveryGridViewModel(activity?.application as TsuApplication, this)
    }

    val exoPlayer: SimpleExoPlayer by lazy {
        requireContext().createAppExoPlayer()
    }

    val discoveryGridAdapter: DiscoveryGridAdapter by lazy {
        DiscoveryGridAdapter( exoPlayer, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentComponent =
            (activity?.application as TsuApplication).appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        // search setup
        val view = inflater.inflate(R.layout.search, container, false)
        val viewManager = LinearLayoutManager(context)
        view.findViewById<RecyclerView>(R.id.search_results).apply {
            layoutManager = viewManager
            adapter = searchResultsAdapter
            if(searchTerm.isEmpty()) hide()
        }

        progressBar = view.findViewById(R.id.progress_bar)
        progressBar.hide()

        val searchQuery = view.findViewById<TextInputEditText>(R.id.searchQuery)
        searchQuery.setHint(R.string.search)
        searchQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (requireActivity().isInternetAvailable()) {
                    Log.i("SEARCH", "after ${s.toString()}")
                    val trim = searchQuery.text.toString().trim()
                    searchTerm = searchQuery.text.toString().trim()
                    if (trim.isEmpty()) {
                        displaySearchResults(emptyList())

                    } else {
                        autoCompletePublishSubject.onNext(trim)
                        Log.d("SEARCH", "Disposables = ${compositeDisposable.size()}")
                    }
                } else {
                    if (mToast != null)
                        mToast?.cancel()
                    mToast = Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.connectivity_issues_message),
                        Toast.LENGTH_LONG
                    )
                    mToast?.show()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //NO-OP
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //NO-OP
            }

        })
        searchQuery.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                dismissKeyboard()
                return@OnEditorActionListener true
            }
            false
        })

        // discovery setup
        val discoveryViewManager = SpannedGridLayoutManager(SpannedGridLayoutManager.Orientation.VERTICAL, 3)
        discoveryViewManager.spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup { position ->
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

        discoveryRecyclerView = view.findViewById<RecyclerView>(R.id.discovery_feed_recycler).apply {
            layoutManager = discoveryViewManager
            adapter = discoveryGridAdapter

            if(searchTerm.isEmpty())
                show()
            else
                hide()

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
        discoveryRecyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            private val visibleThreshold = 5
            private var lastVisibleItem = 0
            var totalItemCount = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!islastPositionUpdated) {
                    lastItemVisiblePosition = discoveryViewManager.lastVisiblePosition
                    Log.d(DISCOVERY_TAG, "last visible $lastItemVisiblePosition")
                    islastPositionUpdated = true
                }

                totalItemCount = discoveryViewManager.itemCount
                lastVisibleItem = discoveryViewManager.lastVisiblePosition
                if (!discoveryGridAdapter.isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if(discoveryGridViewModel.nextPage != null){
                        Log.d(DISCOVERY_TAG, "asking for page ${discoveryGridViewModel.nextPage}")
                        discoveryGridAdapter.isLoading = true
                        discoveryGridAdapter.notifyDataSetChanged()
                        fetchData(discoveryGridViewModel.nextPage)
                    } else {
                        Log.d(DISCOVERY_TAG, "Not triggering loadmore as next_page is null")
                    }
                }
            }
        })

        if (discoveryGridViewModel.nextPage == 0) {
            fetchData(null)
        } else if (discoveryGridViewModel.nextPage != null) {
            fetchData(discoveryGridViewModel.nextPage)
        }
        discoveryGridViewModel.getDiscoveryCache().observe(viewLifecycleOwner, Observer { items ->
            discoveryGridAdapter.updatePosts(items)
        })

        return view
    }

    private fun fetchData(nextPage: Int?) {
        discoveryGridViewModel.getDiscoveryPosts(nextPage)
    }

    override fun didUpdateDiscoveryPosts(nextPage: Int?) {
        discoveryGridAdapter.isLoading = false
    }

    override fun didErrorWith(message: String) {
        snack(message)
    }

    override fun onStart() {
        postPositionChange()
        super.onStart()
        when (args.searchType) {
            SEARCH_TYPE_MENTION -> configureUsersAutoComplete()
            SEARCH_TYPE_USERS -> configureUsersAutoComplete()
            SEARCH_TYPE_GROUP_TOPICS -> configureGroupTopicsAutoComplete()
            SEARCH_TYPE_GROUPS -> configureGroupsAutoComplete()
            SEARCH_TYPE_HASHTAGS -> configureHashTagAutoComplete()
            else -> configureAnyAutoComplete()
        }
    }

    override fun onResume() {
        postPositionChange()
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        dismissKeyboard()
        if(!searchTerm.isEmpty()) {
            properties["term"] = searchTerm
        }
        analyticsHelper.logEvent("global_search_activated", properties)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mToast?.cancel()
    }

    private fun configureAnyAutoComplete() {
        baseConfigureAutoComplete(SearchResponse(), { str ->
            when {
                str[0] == '#' || str[0] == '@' -> str.substring(1, str.length)
                else -> str
            }
        }, { query ->
            searchApi.searchAny(query)
        }, { response ->
            val result = ArrayList<Any>()
            response.body()?.data?.let {
                result.addAll(it.user)
                result.addAll(it.group)
                result.addAll(it.hashtag)
            }
            result
        })
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        cancel()
        super.onDestroy()
    }

    private fun configureHashTagAutoComplete() {
        baseConfigureAutoComplete(emptyList(), { str ->
            when {
                str[0] == '#' -> str.substring(1, str.length)
                else -> str
            }
        }, { query ->
            searchApi.searchHashtags(query)
        }, { response ->
            response.body()?.data ?: emptyList()
        })
    }

    private fun configureUsersAutoComplete() {
        baseConfigureAutoComplete(SearchUsersResponse(), { str ->
            when {
                str[0] == '@' -> str.substring(1, str.length)
                else -> str
            }
        }, { query ->
            searchApi.searchUsers(query)
        }, { response ->
            response.body()?.data?.users ?: emptyList()
        })
    }

    private fun configureGroupTopicsAutoComplete() {
        baseConfigureAutoComplete(emptyList(), apiCall = { query ->
            searchApi.searchGroups(query, true)
        }, mapResponse = { response ->
            response.body()?.data ?: emptyList()
        })
    }

    private fun configureGroupsAutoComplete() {
        baseConfigureAutoComplete(emptyList(), apiCall = { query ->
            searchApi.searchGroups(query, false)
        }, mapResponse = { response ->
            response.body()?.data ?: emptyList()
        })
    }

    private fun <T : Any> baseConfigureAutoComplete(
        emptyResponse: T,
        filter: (String) -> String = { str -> str },
        apiCall: (query: String) -> Single<Response<DataWrapper<T>>>,
        mapResponse: (query: Response<DataWrapper<T>>) -> List<Any>
    ) {
        compositeDisposable += autoCompletePublishSubject
            .debounce(
                AUTO_SUGGEST_THROTTLE_INTERVAL,
                AUTO_SUGGEST_THROTTLE_UNIT
            )
            .distinctUntilChanged()
            .doOnNext {
                Log.i("SEARCH", "subject - before $it")
            }
            .filter { str -> str.length > 1 }
            .doOnNext {
                Log.i("SEARCH", "searching for  $it")
            }
            .map { str ->
                val result = filter(str)
                Log.i("SEARCH", "searching filtered  $str")
                result
            }
            .switchMapSingle {
                apiCall(it).onErrorResumeNext { t ->
                    Log.e("SEARCH", "Failed3 to get search results", t)
                    Single.just(Response.success(DataWrapper(emptyResponse)))
                }
            }
            .subscribeOn(schedulers.io())
            .doOnNext {
                Log.i("SEARCH", "HTTP response for  ${it.code()}")
            }
            .map { resp ->
                mapResponse(resp)
            }
            .observeOn(schedulers.main())
            .onErrorReturn { throwable ->
                Log.e("SEARCH", "Failed22 to get search results", throwable)
                emptyList()
            }.subscribe({ result ->
                displaySearchResults(result)
            }, { t: Throwable? ->
                Log.e("SEARCH", "Failed to get search results", t)
            })
    }

    private fun displaySearchResults(result: List<Any>) {
        Log.d("SEARCH", "response = $result")
        launch {
            searchResultsAdapter.updateSearchResults(result)
            withContext(Dispatchers.Main){
                search_results?.smoothScrollToPosition(0)
            }
        }
        if(result.isNotEmpty()){
            //show search list
            view?.findViewById<RecyclerView>(R.id.discovery_feed_recycler).hide()
            search_results.show()
        } else {
            if(searchTerm.isEmpty()) {
                view?.findViewById<RecyclerView>(R.id.discovery_feed_recycler).show()
                search_results.hide()
            }

        }
    }

    override fun onPostClicked(post: Post, position: Int) {
        postPosition = position
        val action = DiscoveryFeedFragmentDirections.showDiscoveryFeedFragmentt()
        action.postId = post.id
        action.postPosition = position
        action.userId = -1
        findNavController().navigate(action)
    }

    private fun postPositionChange() {
        if (postPosition != null) {
            discoveryRecyclerView.layoutManager?.scrollToPosition(postPosition!!)
            postPosition = null
        }
    }

}