package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.BlockApi
import social.tsu.android.network.model.BlockUserPayload
import social.tsu.android.network.model.UnblockUserPayload
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

interface BlockServiceCallback : DefaultServiceCallback {
    fun didCompleteUserBlock(userId: Int, message: String)
    fun didCompleteUserUnblockResponse(userId: Int, message: String)
    fun didFailToBlockUser(userId: Int, message: String)
    fun didFailToUnblockUser(userId: Int, message: String)
}

abstract class BlockService : DefaultService() {
    abstract fun blockUser(userId: Int)
    abstract fun unblockUser(userId: Int)
}

class DefaultBlockService(
    private val application: TsuApplication,
    private val callback: BlockServiceCallback?
) : BlockService() {

    @Inject
    lateinit var blockApi: BlockApi

    @Inject
    lateinit var schedulers: RxSchedulers

    override val tag: String
        get() = "DefaultBlockService"

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    init {
        application.appComponent.inject(this)
    }

    override fun blockUser(userId: Int) {
        compositeDisposable += blockApi.blockUser(BlockUserPayload(userId))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        callback?.didCompleteUserBlock(userId, result.message)
                    },
                    onFailure = { errMsg ->
                        Log.e(tag, "Error getting block response data from body $response")
                        callback?.didFailToBlockUser(userId, errMsg)
                    }
                )
            }, { err ->
                callback?.didFailToBlockUser(userId, err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error blocking post", err)
            })
    }

    override fun unblockUser(userId: Int) {
        compositeDisposable += blockApi.unblockUser(UnblockUserPayload(userId))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        callback?.didCompleteUserUnblockResponse(userId, result.message)
                    },
                    onFailure = { errMsg ->
                        Log.e(tag, "Error getting unblock response data from body $response")
                        callback?.didFailToUnblockUser(userId, errMsg)
                    }
                )
            }, { err ->
                callback?.didFailToUnblockUser(userId, err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error unblocking user", err)
            })
    }

}
