package social.tsu.android.service

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.models.ChatUserData
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.AuthenticationApi
import social.tsu.android.network.api.UserSettingsApi
import social.tsu.android.network.model.ChatTokenResponse
import social.tsu.android.network.model.CreateAccountResponse
import social.tsu.android.network.model.LoginRequest
import social.tsu.android.network.model.TokenUpdateRequest
import social.tsu.android.rx.plusAssign
import social.tsu.android.utils.sanitizeUserName

class AuthenticationService(
    private val application: TsuApplication,
    private val authenticationApi: AuthenticationApi,
    private val userSettingsApi: UserSettingsApi,
    private val schedulers: RxSchedulers
) : DefaultService() {

    override val tag: String = "AuthService"
    override val compositeDisposable = CompositeDisposable()

    fun authenticate(
        payload: LoginRequest,
        success: (response: CreateAccountResponse) -> Unit,
        error: (t: Throwable) -> Unit
    ) {
        compositeDisposable += authenticationApi.login(payload)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe({ resp ->
                handleResponse(
                    application,
                    resp,
                    onSuccess = { authResult ->
                        AuthenticationHelper.update(authResult?.data)
                        FirebaseCrashlytics.getInstance().setUserId(
                            "id: ${authResult.data?.id} username: ${authResult.data?.username}"
                        )
                        success(authResult)
                        authResult.data?.let { tsuUser ->
                            generateChatsToken(sanitizeUserName(tsuUser.username!!), object : ServiceCallback<ChatTokenResponse> {
                                override fun onSuccess(result: ChatTokenResponse) {

                                    val chatUserData = ChatUserData(
                                        tsuUser.id ?: 0,
                                        tsuUser.username ?: "",
                                        tsuUser.fullName ?: "",
                                        tsuUser.profilePic,
                                        tsuUser.verified
                                    )
                                    AuthenticationHelper.setupLiveChatsUser(
                                        application,
                                        chatUserData,
                                        result.token
                                    )
                                }

                                override fun onFailure(errMsg: String) {

                                }
                            })
                        }

                        updateFcmToken(authResult.data?.id)
                    },
                    onFailure = { errMsg ->
                        Log.e(tag, "Error: $errMsg ")
                        error(Throwable(errMsg))
                    }
                )
            },
                { t: Throwable ->

                    error(Throwable(t.getNetworkCallErrorMessage(application)))
                }
            )

    }

    fun generateChatsToken(
        username: String,
        serviceCallback: ServiceCallback<ChatTokenResponse>
    ) {
        compositeDisposable += userSettingsApi.generateChatToken(username)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe(
                { resp ->
                    handleResponseWithWrapper(application, resp, serviceCallback)
                },
                { t: Throwable ->
                    serviceCallback.onFailure(t.getNetworkCallErrorMessage(application))
                    Log.e(tag, "Error: ${t.message} ")
                }
            )

    }

    private fun updateFcmToken(userId: Int?) {
        userId?.let { id ->
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(tag, "updateFcmToken failed.", task.exception)
                    return@addOnCompleteListener
                }

                task.result?.token?.let { token ->
                    compositeDisposable += userSettingsApi.updateDeviceToken(
                        id,
                        TokenUpdateRequest("android", token)
                    )
                        .observeOn(schedulers.io())
                        .subscribeOn(schedulers.io())
                        .subscribe({
                            Log.d(tag, "Update token success")
                        }, { t: Throwable ->
                            Log.e(tag,"Update token failed with error: ${t.message}")
                        })
                }
            }
        }
    }

}
