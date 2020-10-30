package social.tsu.android.service.post_feed

import android.app.Application
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.SupportApi
import social.tsu.android.network.model.SupportResponse
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.DefaultService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.handleApiCallError
import social.tsu.android.service.handleResponse
import javax.inject.Inject

abstract class PostSupportService : DefaultService(){
    abstract fun support(postId: Long, serviceCallback: ServiceCallback<SupportResponse>)
}

class DefaultPostSupportService @Inject constructor(
    private val application: Application,
    private val supportApi: SupportApi,
    private val schedulers: RxSchedulers
) : PostSupportService() {

    override val tag: String
        get() = "DefaultPostSupportService"

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun support(postId: Long, serviceCallback: ServiceCallback<SupportResponse>) {
        compositeDisposable += supportApi.support(postId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(application, response, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }

}
