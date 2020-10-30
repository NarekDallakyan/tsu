package social.tsu.android.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.model.Comment
import social.tsu.android.network.model.User

interface CommentsAdapterDelegate {
    var numberOfComments: Int
    var lastPayloadSize: Int

    fun commentAtIndex(index: Int): Comment?
    fun postForComment(): Post?
    fun onBottomReachedListener(lastComment: Comment)
}

class CommentsAdapter(
    private val application: TsuApplication,
    private val actions: ViewHolderActions,
    private val delegate: CommentsAdapterDelegate
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context
    private val COMMENT_TYPE_ITEM = 0
    private val LOADING_TYPE_ITEM = 1

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    override fun getItemCount(): Int {
        return delegate.numberOfComments
    }

    override fun getItemViewType(position: Int): Int {
        val viewType =
            if (position >= delegate.numberOfComments - 1 && delegate.lastPayloadSize == 25)
                LOADING_TYPE_ITEM
            else
                COMMENT_TYPE_ITEM
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            COMMENT_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.comment_item_layout, parent, false)
                CommentsViewHolder(application, actions, view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.comment_loading_item_layout, parent, false)
                CommentsLoadingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CommentsViewHolder) {
            if (position == 0) {
                delegate.postForComment()?.let {
                    holder.updateWithPost(it)
                } ?: run {
                    Log.e("CommentsAdapter", "failed to get a post for the comments")
                }
            } else {
                delegate.commentAtIndex(position)?.let {
                    holder.updateWithComment(it)
                    if (position >= delegate.numberOfComments - 1) {
                        delegate.onBottomReachedListener(it)
                    }
                } ?: run {
                    Log.e("CommentsAdapter", "failed to get comment at position : ${position}")
                }
            }
        } else if (holder is CommentsLoadingViewHolder) {
            delegate.commentAtIndex(position)?.let {
                if (position >= delegate.numberOfComments - 1) {
                    delegate.onBottomReachedListener(it)
                }
            }
        }
    }

    interface ViewHolderActions {
        fun likeComment(comment: Comment)
        fun unlikeComment(comment: Comment)
        fun deleteComment(comment: Comment)
        fun onUserTap(user: User)
        fun onHashtagTap(hashtag: String)
        fun onTagUserTap(tagUser :String)
        fun enableDeleteComment(comment: Comment): Boolean
    }
}