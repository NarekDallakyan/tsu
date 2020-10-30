package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.BlockApi
import social.tsu.android.network.api.UserApi
import social.tsu.android.network.model.BlockUserPayload
import social.tsu.android.network.model.ReportUserPayload
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

interface ReportServiceCallback : DefaultServiceCallback {
    fun didCompleteUserReport(userId: Int, message: String)
    fun didFailToReportUser(userId: Int, message: String)
}

abstract class ReportService : DefaultService() {

    abstract fun reportUser(userId: Int, type: Int)
}

class DefaultReportService(private val application: TsuApplication,
                           private val callback: ReportServiceCallback?) : ReportService() {

    override val tag: String
        get() = "ReportService"
    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    @Inject
    lateinit var userApi: UserApi

    @Inject
    lateinit var schedulers: RxSchedulers

    init {
        application.appComponent.inject(this)
    }

    override fun reportUser(userId: Int, type: Int) {
        compositeDisposable += userApi.reportUser(ReportUserPayload(userId, type))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        callback?.didCompleteUserReport(userId, result.data.message ?: "")
                    },
                    onFailure = { errMsg ->
                            Log.e(tag, "Error getting report response data from body $response")
                        callback?.didFailToReportUser(userId, errMsg)
                    }
                )
            }, { err ->
                callback?.didFailToReportUser(userId, err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error report user", err)
            })
    }
}