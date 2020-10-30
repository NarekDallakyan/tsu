package social.tsu.android.service

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.models.OldUserDetails
import social.tsu.android.network.api.CreateAccountApi
import social.tsu.android.network.model.VerifyInviteRequest
import social.tsu.android.network.model.VerifyInviteResponse
import social.tsu.android.network.model.VerifyOldTsuUserRequest
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


abstract class UserInviteService : DefaultService() {

    abstract fun verifyUserInvite(
        email: String,
        serviceCallback: ServiceCallback<VerifyInviteResponse>
    )


    abstract fun verifyOldTsuUserInvite(
        email: String,
        verificationCode: Int,
        serviceCallback: ServiceCallback<OldUserDetails>
    )

}

class DefaultUserInviteService @Inject constructor(
    private val application: Application,
    private val createAccountApi: CreateAccountApi,
    private val schedulers: RxSchedulers
) : UserInviteService() {

    override val tag: String = "DefaultUserInviteService"

    override val compositeDisposable = CompositeDisposable()

    override fun verifyUserInvite(
        email: String,
        serviceCallback: ServiceCallback<VerifyInviteResponse>
    ) {
        compositeDisposable += createAccountApi.verifyInviteRequest(VerifyInviteRequest(email))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error requesting old user details status", err)
            })

    }

    override fun verifyOldTsuUserInvite(
        email: String,
        verificationCode: Int,
        serviceCallback: ServiceCallback<OldUserDetails>
    ) {
        compositeDisposable += createAccountApi.verifyOldUserRequest(
            VerifyOldTsuUserRequest(
                email,
                verificationCode
            )
        )
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseWithWrapper(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error requesting old user details status", err)
            })

    }

}
