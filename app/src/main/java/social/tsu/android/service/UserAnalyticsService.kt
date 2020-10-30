package social.tsu.android.service

import android.util.Log
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.api.AnalyticsApi
import social.tsu.android.network.model.AnalyticsResponse
import social.tsu.android.rx.plusAssign
import social.tsu.android.utils.errorFromResponse
import javax.inject.Inject


interface UserAnalyticsServiceCallback : DefaultServiceCallback {

    fun didLoadUserAnalytics(data: AnalyticsResponse)
}

abstract class UserAnalyticsService : DefaultService() {

    abstract var callback: UserAnalyticsServiceCallback?
    
    abstract fun loadUserAnalytics(date: String)
}

class DefaultUserAnalyticsService(application: TsuApplication) : UserAnalyticsService() {

    override var callback: UserAnalyticsServiceCallback? = null

    @Inject
    lateinit var analyticsApi: AnalyticsApi

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var moshi: Moshi

    override val tag: String = "DefaultUserAnalyticsService"

    override val compositeDisposable = CompositeDisposable()

    init {
        application.appComponent.inject(this)
    }

    override fun loadUserAnalytics(date: String) {
        compositeDisposable += analyticsApi.getAnalytics(date)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                val data = response.body()?.data
                if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE && data != null) {
                    callback?.didLoadUserAnalytics(data)
                } else {
                    val error = moshi.errorFromResponse(response)?.message ?: response.message()
                    callback?.didErrorWith(error ?: "")
                }
            }, { error ->
                callback?.didErrorWith(error.message ?: "")
                Log.e(tag, "Can't load analytics", error)
            })
    }
}