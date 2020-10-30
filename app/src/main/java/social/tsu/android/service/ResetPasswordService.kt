package social.tsu.android.service.reset_password

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.ResetPasswordApi
import social.tsu.android.network.model.ResetPasswordRequest
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.DefaultService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.handleApiCallError
import social.tsu.android.service.handleResponseResult
import javax.inject.Inject


abstract class ResetPasswordService: DefaultService(){

    abstract fun requestOTP(email:String, serviceCallback: ServiceCallback<Boolean>)

    abstract fun resetPassword(email:String, newPassword:String, code:String, serviceCallback: ServiceCallback<Boolean>)

}

class DefaultResetPasswordService @Inject constructor(
    private val application: Application,
    private val resetPasswordApi: ResetPasswordApi,
    private val schedulers: RxSchedulers
) : ResetPasswordService() {

    override val tag: String = "DefaultResetPasswordService"

    override val compositeDisposable = CompositeDisposable()

    override fun requestOTP(email: String, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += resetPasswordApi.requestOTP(email)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error requesting otp status", err)
            })
    }

    override fun resetPassword(email: String, newPassword: String, code: String, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += resetPasswordApi.resetUserPassword(ResetPasswordRequest(email,newPassword,code))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error resetting password status", err)
            })
    }
}
