package social.tsu.android.ui.post_feed.comment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import dagger.android.support.AndroidSupportInjection
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.CommentDefaultInputAdapter
import social.tsu.android.adapters.CommentDefaultInputAdapterDelegate
import social.tsu.android.adapters.CommentsAdapter
import social.tsu.android.adapters.CommentsAdapterDelegate
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.PostsCache
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.Comment
import social.tsu.android.network.model.User
import social.tsu.android.observeOnce
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.search.MENTION_TYPE
import social.tsu.android.ui.search.SearchFragment
import social.tsu.android.ui.showKeyboard
import social.tsu.android.ui.snack
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.viewModel.MentionViewModel
import social.tsu.android.viewModel.comments.CommentsViewModel
import social.tsu.android.viewModel.comments.CommentsViewModelCallback
import social.tsu.android.viewModel.comments.DefaultCommentsViewModel
import javax.inject.Inject
import kotlin.properties.Delegates

const val DEFAULT_LOAD_MORE_SIZE = 25

class CommentsFragment : Fragment(), CommentsViewModelCallback, CommentsAdapterDelegate,
    CommentDefaultInputAdapterDelegate, CommentsAdapter.ViewHolderActions {

    private val commentsAdapter: CommentsAdapter by lazy {
        CommentsAdapter(activity?.application as TsuApplication, this, this)
    }

    private val emojiAdaper: CommentDefaultInputAdapter by lazy {
        CommentDefaultInputAdapter(activity?.application as TsuApplication, this)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    private val commentActionsViewModel by viewModels<CommentViewModel> { viewModelFactory }
    val properties = HashMap<String, Any?>()

    override var numberOfComments: Int = 0
        get() = viewModel.numberOfComments

    override var lastPayloadSize: Int = 0
        get() = viewModel.lastPayloadSize

    private val args: CommentsFragmentArgs by navArgs()
    private var postId by Delegates.notNull<Long>()
    private lateinit var recyclerView: RecyclerView

    private var mentionViewModel: MentionViewModel? = null
    private var commentEditText: EditText? = null


    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {

        }

        override fun beforeTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int
        ) {

        }

        override fun afterTextChanged(arg0: Editable) {

            var value = arg0.toString()
            if (value.isNullOrEmpty()) {
                return
            }

            if (value.contains("@")) {
                val split = value.split(" ")
                for (item in split) {
                    if (item.contains("@") && item.length == 1) {
                        initialText = commentEditText?.text.toString()
                        openMentionSearchFragment()
                        break
                    }
                }
            }

        }
    }


    var initialText: String? = null

    private lateinit var viewModel: CommentsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = DefaultCommentsViewModel(activity?.application as TsuApplication, this)

        postId = args.postId
        commentActionsViewModel.postId = args.postId
        val view = inflater.inflate(
            R.layout.comments_layout,
            container, false
        )

        commentEditText = view.findViewById(R.id.comments_text_input) as EditText

        mentionViewModel = ViewModelProvider(requireActivity()).get(MentionViewModel::class.java)


        commentEditText?.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    commentEditText?.addTextChangedListener(textWatcher)
                } else {
                    commentEditText?.removeTextChangedListener(textWatcher)
                }
            }

        val viewManager = LinearLayoutManager(context)
        recyclerView = view.findViewById<RecyclerView>(R.id.comments_recycler_view).apply {
            layoutManager = viewManager
            adapter = commentsAdapter
        }

        val commentsManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        view.findViewById<RecyclerView>(R.id.comments_default_recycler_view).apply {
            layoutManager = commentsManager
            adapter = emojiAdaper
        }

        view.findViewById<TextView>(R.id.comment_reply_button)?.setOnClickListener {
            userDidTapToPost()
        }

        commentActionsViewModel.post.observeOnce {
            PostsCache[it.id] = it
            commentsAdapter.notifyItemChanged(0)
            // if it is shared post, take original timestamp else timestamp
            val timestamp = it.original_timestamp ?: it.timestamp
            viewModel.getCommentsForPost(it.id, timestamp)
        }
        properties["postId"] = args.postId
        analyticsHelper.logEvent("comments_viewed", properties)

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        commentEditText?.setText("")
        mentionViewModel?.selectTag("")
    }

    private fun openMentionSearchFragment() {
        dismissKeyboard()
        initialText = commentEditText?.text.toString()
        commentEditText?.isSelected = false
        commentEditText?.isFocusable = false


        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.commentsFragment) {
            navController.navigate(
                R.id.action_commentFragment_To_mentionSearchFragment,
                bundleOf(
                    "searchType" to SearchFragment.SEARCH_TYPE_MENTION,
                    MENTION_TYPE to SearchFragment.MENTION_TYPE_COMMENT
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()

        var tag = ""
        mentionViewModel?.getTag()?.observe(requireActivity(), Observer {
            if (!it.isNullOrEmpty()) tag = it
        })
        mentionViewModel?.selectTag("")
        if (!initialText.isNullOrEmpty()) {
            if (!tag.isNullOrEmpty()) {
                commentEditText?.append(initialText + tag)
            } else {
                commentEditText?.append(initialText)
            }
            commentEditText?.post(Runnable {
                commentEditText?.requestFocus()
                activity?.showKeyboard()
            })
        }

        initialText = ""
    }


    override fun onStart() {
        super.onStart()
        commentEditText?.setText("")
    }

    fun userDidTapToPost() {
        dismissKeyboard()
        if (requireActivity().isInternetAvailable()) {
            view?.findViewById<TextView>(R.id.comments_text_input)?.text?.let {
                viewModel.createCommentForPost(postId, it.toString())
                properties["postId"] = args.postId
                analyticsHelper.logEvent("comment_created", properties)
            }
            view?.findViewById<TextView>(R.id.comments_text_input)?.text = ""
        } else
            requireActivity().internetSnack()
    }

    override fun commentAtIndex(index: Int): Comment? {
        return viewModel.getComentAt(index - 1)
    }

    override fun postForComment(): Post? {
        viewModel.postId = postId
        return viewModel.post
    }

    override fun onBottomReachedListener(lastComment: Comment) {
        if (viewModel.lastPayloadSize == DEFAULT_LOAD_MORE_SIZE)
            viewModel.getMoreCommentsForPost(lastComment.postId.toLong(), lastComment.id)
    }

    override fun didTapOnEmoji(value: String) {
        view?.findViewById<TextInputEditText>(R.id.comments_text_input)?.let {
            val position = it.selectionStart
            it.text?.let { text ->
                val first: String = text.substring(0, position)
                val second: String = text.substring(position)
                val final = first.plus(value).plus(second)
                it.setText(final)
            } ?: kotlin.run {
                it.append(value)
            }
            it.setSelection(position.plus(value.length))
        }
    }

    override fun completedCommentsUpdate() {
        commentsAdapter.notifyDataSetChanged()
    }

    override fun completedCreateComment() {
        commentsAdapter.notifyDataSetChanged()
        if (::recyclerView.isInitialized)
            recyclerView.smoothScrollToPosition(0)
    }

    override fun didErrorWith(message: String) {
        requireActivity().snack(message)
    }

    override fun onUserTap(user: User) {
        findNavController().showUserProfile(user.id?.toInt())
        /*  user.id?.toInt()?.let {
             findNavController().navigate(
                 CommentsFragmentDirections.showUserProfile().apply {
                     id = it
                 }
             )
         }*/

    }

    override fun likeComment(comment: Comment) {
        if (requireActivity().isInternetAvailable()) {
            val result = commentActionsViewModel.likeComment(comment)
            result.observe(viewLifecycleOwner, Observer { data ->
                if (data is Data.Success) {
                    val updatedComment = if (comment.hasLiked) comment else {
                        comment.apply {
                            likeCount++
                            hasLiked = true
                        }
                    }
                    val index = viewModel.updateComment(updatedComment)
                    index?.let { commentsAdapter.notifyItemChanged(it + 1) }
                }
            })
        } else
            requireActivity().internetSnack()
    }

    override fun unlikeComment(comment: Comment) {
        if (requireActivity().isInternetAvailable()) {
            val result = commentActionsViewModel.unlikeComment(comment)
            result.observe(viewLifecycleOwner, Observer { data ->
                if (data is Data.Success) {
                    val updatedComment = if (!comment.hasLiked) comment else {
                        comment.apply {
                            likeCount--
                            hasLiked = false
                        }
                    }
                    val index = viewModel.updateComment(updatedComment)
                    index?.let { commentsAdapter.notifyItemChanged(it + 1) }
                }
            })
        } else
            requireActivity().internetSnack()
    }

    override fun deleteComment(comment: Comment) {
        if (requireActivity().isInternetAvailable()) {
            val result = commentActionsViewModel.deleteComment(comment)
            result.observe(viewLifecycleOwner, Observer { data ->
                if (data is Data.Success) {
                    val index = viewModel.deleteComment(comment)
                    index?.let { commentsAdapter.notifyItemRemoved(it + 1) }
                }
            })
        } else
            requireActivity().internetSnack()
    }

    override fun enableDeleteComment(comment: Comment): Boolean {
        val myId = AuthenticationHelper.currentUserId?.toLong()
        return if ((viewModel.post?.is_share == true && viewModel.post?.original_user?.id != myId) ||
            (viewModel.post?.is_share != true && viewModel.post?.user?.id != myId)
        ) {
            comment.user.id == AuthenticationHelper.currentUserId?.toLong()
        } else true
    }

    override fun onHashtagTap(hashtag: String) {
        findNavController().navigate(
            R.id.hashtagGridFragment, bundleOf(
                "hashtag" to hashtag
            )
        )
    }

    override fun onTagUserTap(tagUser: String) {
        findNavController().showUserProfile(tagUser)
    }

}