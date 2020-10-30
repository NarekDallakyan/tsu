package social.tsu.android.service

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.CommentAPI
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


abstract class CommentActionsService: DefaultService(){

    abstract fun likeComment(commentId: Int, serviceCallback: ServiceCallback<Boolean>)

    abstract fun unlikeComment(commentId: Int, serviceCallback: ServiceCallback<Boolean>)

    abstract fun deleteComment(commentId: Int, serviceCallback: ServiceCallback<Boolean>)

}

class DefaultCommentActionsService @Inject constructor(
    private val application: Application,
    private val commentApi: CommentAPI,
    private val schedulers: RxSchedulers
) : CommentActionsService() {

    override val tag: String = "DefaultCommentActionsService"

    override val compositeDisposable = CompositeDisposable()

    override fun likeComment(commentId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += commentApi.likeComment(commentId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error liking comment status", err)
            })
    }

    override fun unlikeComment(commentId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += commentApi.unlikeComment(commentId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error unliking comment status", err)
            })
    }

    override fun deleteComment(commentId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += commentApi.deleteComment(commentId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error deleting comment status", err)
            })
    }

}