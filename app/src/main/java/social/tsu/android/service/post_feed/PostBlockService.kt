package social.tsu.android.service.post_feed

import android.app.Application
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.BlockApi
import social.tsu.android.network.model.BlockUserPayload
import social.tsu.android.network.model.BlockUserResponse
import social.tsu.android.network.model.UnblockUserPayload
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.DefaultService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.handleApiCallError
import social.tsu.android.service.handleResponse
import javax.inject.Inject

abstract class PostBlockService : DefaultService() {
    abstract fun blockUser(userId: Int, serviceCallback: ServiceCallback<String>)
    abstract fun unblockUser(userId: Int, serviceCallback: ServiceCallback<String>)
}

class DefaultPostBlockService @Inject constructor(
    private val application: Application,
    private val blockApi: BlockApi,
    private val schedulers: RxSchedulers
) : PostBlockService() {


    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultPostBlockService"


    override fun blockUser(userId: Int, serviceCallback: ServiceCallback<String>) {
        compositeDisposable += blockApi.blockUser(BlockUserPayload(userId))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(application, response, object : ServiceCallback<BlockUserResponse> {
                    override fun onSuccess(result: BlockUserResponse) {
                        serviceCallback.onSuccess(result.message)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }

    override fun unblockUser(userId: Int, serviceCallback: ServiceCallback<String>) {
        compositeDisposable += blockApi.unblockUser(UnblockUserPayload(userId))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(application, response, object : ServiceCallback<BlockUserResponse> {
                    override fun onSuccess(result: BlockUserResponse) {
                        serviceCallback.onSuccess(result.message)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }


}