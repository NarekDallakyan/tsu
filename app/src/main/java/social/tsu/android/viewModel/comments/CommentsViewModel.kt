package social.tsu.android.viewModel.comments

import android.util.Log
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.PostsCache
import social.tsu.android.network.model.Comment
import social.tsu.android.service.CommentsService
import social.tsu.android.service.CommentsServiceCallback
import social.tsu.android.service.DefaultCommentsService

interface CommentsViewModelCallback {
    fun completedCommentsUpdate()
    fun completedCreateComment()
    fun didErrorWith(message: String)
}

abstract class CommentsViewModel : CommentsServiceCallback {

    abstract var numberOfComments: Int
    abstract var lastPayloadSize: Int
    abstract var post: Post?
    abstract var postId: Long

    abstract fun getComentAt(index: Int): Comment?
    abstract fun updateComment(comment: Comment): Int?
    abstract fun deleteComment(comment: Comment): Int?
    abstract fun getCommentsForPost(postId: Long, postTimestamp: Int)
    abstract fun getMoreCommentsForPost(postId: Long, lastCommentTimestamp: Int)
    abstract fun createCommentForPost(postId: Long, content: String)

}

class DefaultCommentsViewModel(application: TsuApplication, private var callback: CommentsViewModelCallback?): CommentsViewModel() {

    // TODO: add feed service to refresh post with newly added comment

    private val commentsService: CommentsService by lazy {
        DefaultCommentsService(application, this)
    }

    private val postCache: PostsCache = PostsCache
    private val lastCommentsPayload = mutableListOf<Comment>()

    override var postId: Long = 0

    override var numberOfComments: Int = 0
        get() = comments.count() + 1

    override var lastPayloadSize: Int = 0
        get() = lastCommentsPayload.size

    override var post: Post? = null
        get() = postCache[this.postId]

    private var postRemovedCounter = 0

    private var comments: MutableList<Comment> = mutableListOf()

    override fun getCommentsForPost(postId: Long, postTimestamp: Int) {
        this.postId = postId
        commentsService.getComments(postId, postTimestamp)
    }

    override fun getMoreCommentsForPost(postId: Long, lastCommentId: Int) {
        commentsService.getMoreComments(postId, lastCommentId)
    }

    override fun createCommentForPost(postId: Long, content: String) {
        commentsService.createComment(postId, content)
    }

    override fun getComentAt(index: Int): Comment? {
        if (comments.isEmpty()) {
            return null
        }

        if (index < 0 || index > comments.count()) {
            return null
        }
        return comments[index]
    }

    /**
     *
     *  No need to add comment object into the index here
     *  it causes crashes. Leaving it in for now, for safe-keeping
     *  //comments[index] = comment
     *
     */
    override fun updateComment(comment: Comment): Int? {
        return if (!comments.isNullOrEmpty()) {
            val index = comments.indexOfFirst { it.id == comment.id }
           // comments[index] = comment
            index
        } else null
    }

    override fun deleteComment(comment: Comment): Int? {
        return if (!comments.isNullOrEmpty()) {
            val index = comments.indexOfFirst { it.id == comment.id }
            comments.removeAt(index)
            postRemovedCounter++
            index
        } else null
    }

    override fun completedCreateCommentForPost(comment: Comment) {
        post?.let {
            this.comments.add(0, comment)
            callback?.completedCreateComment()
        }
    }

    override fun completedGetCommentsForPost(comments: List<Comment>) {
        lastCommentsPayload.apply {
            clear()
            addAll(comments)
        }
        this.comments = comments.toMutableList()
        callback?.completedCommentsUpdate()
    }

    override fun completedGetMoreCommentsForPost(response: List<Comment>) {
        lastCommentsPayload.apply {
            clear()
            addAll(response)
        }
        val set = mutableSetOf<Comment>()
        set.addAll(comments)
        set.addAll(response)
        this.comments.clear()
        this.comments.addAll(set)
        callback?.completedCommentsUpdate()
    }

    override fun didErrorWith(message: String) {
        Log.e("CommentsViewModel", message)
        callback?.didErrorWith(message)
    }

}