package social.tsu.android.service.post_feed

import android.app.Application
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.ShareAPI
import social.tsu.android.network.model.ShareResponseDTO
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.*
import javax.inject.Inject

abstract class PostShareService : DefaultService() {
    abstract fun sharePost(postId: Long, serviceCallback: ServiceCallback<ShareResponseDTO>)
    abstract fun unsharePost(postSharedId: Int, serviceCallback: ServiceCallback<Boolean>)
}

class DefaultPostShareService @Inject constructor(
    private val application: Application,
    private val shareAPI: ShareAPI,
    private val schedulers: RxSchedulers
) : PostShareService() {


    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultPostShareService"

    override fun sharePost(postId: Long, serviceCallback: ServiceCallback<ShareResponseDTO>) {
        compositeDisposable += shareAPI.shareUserPost(postId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponseWithWrapper(application, response, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }

    override fun unsharePost(postSharedId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += shareAPI.unshareUserPost(postSharedId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponseResult(application, response, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }


}