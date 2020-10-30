package social.tsu.android.service.post_feed

import android.app.Application
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.LikeApi
import social.tsu.android.network.model.LikeResponse
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.DefaultService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.handleApiCallError
import social.tsu.android.service.handleResponse
import javax.inject.Inject

abstract class PostLikeService : DefaultService() {
    abstract fun like(postId: Long, serviceCallback: ServiceCallback<LikeResponse>)
    abstract fun unlike(postId: Long, serviceCallback: ServiceCallback<LikeResponse>)
}

class DefaultPostLikeService @Inject constructor(
    private val application: Application,
    private val likeApi: LikeApi,
    private val schedulers: RxSchedulers
) : PostLikeService() {


    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultPostLikeService"

    override fun like(postId: Long, serviceCallback: ServiceCallback<LikeResponse>) {
        compositeDisposable += likeApi.likeUserPost(postId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(application, response, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }

    override fun unlike(postId: Long, serviceCallback: ServiceCallback<LikeResponse>) {
        compositeDisposable += likeApi.unlikeUserPost(postId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(application, response, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }


}