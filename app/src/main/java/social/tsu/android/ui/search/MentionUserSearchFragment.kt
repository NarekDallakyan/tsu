package social.tsu.android.ui.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.search.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.Response
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.SearchApi
import social.tsu.android.TsuApplication
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.*
import social.tsu.android.rx.plusAssign
import social.tsu.android.ui.community.CommunityViewModel
import social.tsu.android.ui.post_feed.community.CommunityFeedFragmentDirections
import social.tsu.android.ui.showKeyboard
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.MentionViewModel
import social.tsu.android.viewModel.SharedViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject


private const val AUTO_SUGGEST_THROTTLE_INTERVAL = 100L
private val AUTO_SUGGEST_THROTTLE_UNIT = TimeUnit.MILLISECONDS

class MentionUserSearchFragment : Fragment(), CoroutineScope by MainScope() {


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

    private val compositeDisposable = CompositeDisposable()
    private val autoCompletePublishSubject = PublishSubject.create<String>()
    private val model: CommunityViewModel by activityViewModels()
    private val mentionViewModel: MentionViewModel by activityViewModels()

    var sharedViewModel: SharedViewModel? = null
    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var searchApi: SearchApi

    val args: MentionUserSearchFragmentArgs by navArgs()

    private val searchResultsAdapter = SearchResultsAdapter(object : OnItemClickListener<Any> {
        override fun onItemClicked(item: View, data: Any) {
            Log.d("CLICK", "You clicked $data")
            when (data) {

                is MentionUser -> {
                    when (args.mentionType) {
                        MENTION_TYPE_MAINPOST -> {
                            mentionViewModel.selected = data.username
                            //mentionViewModel.setMainPostTag(data.username)
                            findNavController().navigateUp()
                            sharedViewModel?.select(false)
                        }

                        MENTION_TYPE_EDIT_POST -> {
                            mentionViewModel.selectTag(data.username)
                            //mentionViewModel.setMainPostTag(data.username)
                            findNavController().navigateUp()
                        }

                        MENTION_TYPE_COMMUNITY -> {
                            mentionViewModel.selectTag(data.username)
                            findParentNavController().popBackStack(R.id.postDraftFragment, false)
                        }
                        MENTION_TYPE_CHAT -> {
                            mentionViewModel.selectTag(data.username)
                            findParentNavController().popBackStack(R.id.chatFragment, false)
                        }
                        MENTION_TYPE_COMMENT -> {
                            mentionViewModel.selectTag(data.username)
                            findParentNavController().popBackStack(R.id.commentsFragment, false)
                        }
                    }

                }

                is SearchUser -> {
                    if (args.searchType == SEARCH_TYPE_MENTION) {
                        if (args.mentionType == MENTION_TYPE_MAINPOST) {
                            mentionViewModel.selected = data.username
                            findNavController().navigateUp()

                        } else if (args.mentionType == MENTION_TYPE_COMMUNITY) {
                            mentionViewModel.selectTag(data.username)
                            findParentNavController().popBackStack(R.id.postDraftFragment, false)
                        } else if (args.mentionType == MENTION_TYPE_CHAT) {
                            mentionViewModel.selectTag(data.username)
                            findParentNavController().popBackStack(R.id.chatFragment, false)
                        } else if (args.mentionType == MENTION_TYPE_COMMENT) {
                            mentionViewModel.selectTag(data.username)
                            findParentNavController().popBackStack(R.id.commentsFragment, false)
                        }

                    } else {
                        Navigation.findNavController(item).showUserProfile(data.id)
                    }
                }
                is Group -> {
                    if (args.searchType == SEARCH_TYPE_GROUP_TOPICS) {
                        model.selectedGroup = data
                        findNavController().navigateUp()
                    } else {
                        val action = CommunityFeedFragmentDirections.openCommunityFeedFragment()
                        action.group = data
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
                    findNavController().navigate(
                        R.id.hashtagGridFragment,
                        bundleOf("hashtag" to data.text)
                    )
                }
            }
        }
    })


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentComponent =
            (activity?.application as TsuApplication).appComponent.fragmentComponent().create()

        fragmentComponent.inject(this)

        val view = inflater.inflate(
            R.layout.search,
            container, false
        )


        val viewManager = LinearLayoutManager(context)
        view.findViewById<RecyclerView>(R.id.search_results).apply {
            layoutManager = viewManager
            adapter = searchResultsAdapter
        }

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        val searchQuery = view.findViewById<TextInputEditText>(R.id.searchQuery)
        searchQuery.setHint(R.string.search)

        searchQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Log.i("SEARCH", "after ${s.toString()}")
                val trim = searchQuery.text.toString().trim()

                if (trim.isEmpty()) {
                    displaySearchResults(emptyList())
                } else {
                    autoCompletePublishSubject.onNext(trim)
                    Log.d("SEARCH", "Disposables = ${compositeDisposable.size()}")
                }


            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //NO-OP
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //NO-OP
            }

        })
        searchQuery.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                dismissKeyboard()
                return@OnEditorActionListener true
            }
            false
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        searchQuery.post(Runnable {
            searchQuery.requestFocus()
            val imgr: InputMethodManager =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imgr.showSoftInput(searchQuery, InputMethodManager.SHOW_IMPLICIT)
        })
    }

    override fun onStart() {
        super.onStart()
        requireActivity().showKeyboard()
        when (args.searchType) {
            SEARCH_TYPE_MENTION -> configureMentionUsersAutoComplete()
            SEARCH_TYPE_USERS -> configureUsersAutoComplete()
            SEARCH_TYPE_GROUP_TOPICS -> configureGroupTopicsAutoComplete()
            SEARCH_TYPE_GROUPS -> configureGroupsAutoComplete()
            SEARCH_TYPE_HASHTAGS -> configureHashTagAutoComplete()
            else -> configureAnyAutoComplete()
        }
    }

    override fun onStop() {
        super.onStop()
        dismissKeyboard()
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
        dismissKeyboard()
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

    private fun configureMentionUsersAutoComplete() {
        baseConfigureAutoComplete(emptyList(), { str ->
            when {
                str[0] == '@' -> str.substring(1, str.length)
                else -> str
            }
        }, { query ->
            searchApi.searchMentionUsers(query)
        }, { response ->
            response.body()?.data ?: emptyList()
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
        }
    }

}