package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.ShareAPI
import social.tsu.android.network.model.ShareResponseDTO
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

interface ShareServiceCallback : DefaultServiceCallback {
    fun didCompletePostShare(response: ShareResponseDTO)
    fun didCompletePostUnshare(response: ShareResponseDTO)
    fun didFailToSharePost(message: String)
}

abstract class ShareService : DefaultService() {
    abstract fun share(postId: Long)
    abstract fun unshare(postSharedId: Int)
}

class DefaultShareService(
    private val application: TsuApplication,
    private val callback: ShareServiceCallback?
) : ShareService() {

    @Inject
    lateinit var shareApi: ShareAPI

    @Inject
    lateinit var schedulers: RxSchedulers

    override val tag: String
        get() = "DefaultShareService"

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    init {
        application.appComponent.inject(this)
    }

    override fun share(postId: Long) {
        compositeDisposable += shareApi.shareUserPost(postId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        callback?.didCompletePostShare(result.data)
                    },
                    onFailure = { errMsg ->
                        Log.e(tag, "Error getting share response data from body $response")
                        callback?.didFailToSharePost(errMsg)
                    }
                )
            }, { err ->
                callback?.didFailToSharePost(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error sharing post", err)
            })
    }

    override fun unshare(postSharedId: Int) {
        compositeDisposable += shareApi.unshareUserPost(postSharedId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        callback?.didCompletePostUnshare(result.data)
                    },
                    onFailure = { errMsg ->
                        Log.e(tag, "Error getting unshare response data from body $response")
                        callback?.didFailToSharePost(errMsg)
                    }
                )
            }, { err ->
                callback?.didFailToSharePost(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error sharing post", err)
            })
    }

}