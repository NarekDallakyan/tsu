package social.tsu.android.service

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.LogoutApi
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


abstract class LogoutService: DefaultService(){

    abstract fun logout( serviceCallback: ServiceCallback<Boolean>)

}

class DefaultLogoutService @Inject constructor(
    private val application: Application,
    private val logoutApi: LogoutApi,
    private val schedulers: RxSchedulers
) : LogoutService() {

    override val tag: String = "DefaultLogoutService"

    override val compositeDisposable = CompositeDisposable()

    override fun logout(serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += logoutApi.logoutUser()
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error logging out status", err)
            })
    }

}
