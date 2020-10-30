package social.tsu.android.service

import android.util.Log
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.api.AnalyticsApi
import social.tsu.android.network.model.FamilyTreeResponse
import social.tsu.android.rx.plusAssign
import social.tsu.android.utils.errorFromResponse
import javax.inject.Inject


interface UserFamilyTreeServiceCallback : DefaultServiceCallback {

    fun didLoadUserFamilyTree(data: FamilyTreeResponse) {}
}

abstract class UserFamilyTreeService : DefaultService() {

    abstract var callback: UserFamilyTreeServiceCallback?

    abstract fun loadUserFamilyTree(page: Int, count: Int)
}

class DefaultUserFamilyTreeService(application: TsuApplication) : UserFamilyTreeService() {

    override var callback: UserFamilyTreeServiceCallback? = null

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

    override fun loadUserFamilyTree(
        page: Int,
        count: Int
    ) {
        compositeDisposable += analyticsApi.getFamilyTree(page, count)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                val data = response.body()?.data
                if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE && data != null) {
                    callback?.didLoadUserFamilyTree(data)
                } else {
                    val error = moshi.errorFromResponse(response)?.message ?: response.message()
                    callback?.didErrorWith(error ?: "")
                }
            }, { error ->
                callback?.didErrorWith(error.message ?: "")
                Log.e(tag, "Can't load family tree", error)
            })
    }
}