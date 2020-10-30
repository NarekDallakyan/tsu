package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.execute
import social.tsu.android.network.api.CommentAPI
import social.tsu.android.network.model.Comment
import social.tsu.android.network.model.CommentDTO
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

interface CommentsServiceCallback: DefaultServiceCallback {
    fun completedGetCommentsForPost(comments: List<Comment>)
    fun completedGetMoreCommentsForPost(comments: List<Comment>)
    fun completedCreateCommentForPost(comment: Comment)
}

abstract class CommentsService: DefaultService() {
    abstract fun getComments(postId: Long, postTimestamp: Int)
    abstract fun getMoreComments(postId: Long, lastCommentTimestamp: Int)
    abstract fun createComment(postId: Long, content: String)
}

class DefaultCommentsService(
    private val application: TsuApplication,
    var callback: CommentsServiceCallback?
) : CommentsService() {

    override val tag: String = "DefaultCommentsService"

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    @Inject
    lateinit var commentApi: CommentAPI

    @Inject
    lateinit var postFeedDao: PostFeedDao

    @Inject
    lateinit var schedulers: RxSchedulers

    init {
        application.appComponent.inject(this)
    }

    override fun getComments(postId: Long, postTimestamp: Int) {
        compositeDisposable += commentApi.getPostComments(postId, postTimestamp)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        //yes, looks silly but according to crash we need this check here as somehow
                        //result becomes null and it crashes with NPE in CommentsViewModel
                        //hope that compiler won't optimize this
                        val result = result.data
                        callback?.completedGetCommentsForPost(result)
                    },
                    onFailure = { errMsg ->
                        Log.e(tag, "Error getting comments response data from body $errMsg")
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error creating comment status", err)
            })
    }

    override fun getMoreComments(postId: Long, lastCommentId: Int) {
        compositeDisposable += commentApi.getMorePostComments(postId, lastCommentId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result1 ->
                        //yes, looks silly but according to crash we need this check here as somehow
                        //result becomes null and it crashes with NPE in CommentsViewModel
                        //hope that compiler won't optimize this
                        val result = result1.data
                        callback?.completedGetMoreCommentsForPost(result)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error getting more comments status", err)
            })

    }

    override fun createComment(postId: Long, content: String) {
        val dto = CommentDTO(content)
        compositeDisposable += commentApi.createComment(postId, dto)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        val comment = result.data
                        callback?.completedCreateCommentForPost(comment)
                        execute { postFeedDao.increaseCommentCount(postId) }
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error creating comment status", err)
            })
    }

}