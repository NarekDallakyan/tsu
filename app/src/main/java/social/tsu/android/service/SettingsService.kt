package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.UserSettingsApi
import social.tsu.android.network.model.ProfileEditInfoDTO
import social.tsu.android.network.model.UserProfile
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

const val IS_TEXT_COPY_ENABLED = true

interface SettingsServiceCallback: DefaultServiceCallback {
    fun failedToUpdateUserProfile(message: String?)
    fun completedUserProfileUpdate(info: UserProfile)
}

abstract class SettingsService: DefaultService(), UserInfoServiceCallback {
    abstract fun updateInfo(info: ProfileEditInfoDTO)
}

class DefaultSettingsService(val application: TsuApplication, val callback: SettingsServiceCallback): SettingsService() {

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: UserSettingsApi

    val userService: DefaultUserInfoService by lazy {
        DefaultUserInfoService(application, this)
    }

    override val tag: String
        get() = "DefaultSettingsService"

    override val compositeDisposable = CompositeDisposable()

    init {
        application.appComponent.inject(this)
    }

    override fun didErrorWith(message: String) {
        callback.failedToUpdateUserProfile(message)
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        if (info != null) {
            callback.completedUserProfileUpdate(info)
        } else {
            callback.failedToUpdateUserProfile("Failed to get updated user info")
        }
    }

    override fun updateInfo(info: ProfileEditInfoDTO) {

        compositeDisposable += api.updateUserInfo(info)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        Log.d(tag, "Updated user profile")
                        userService.getUserInfo(result.data.id, true)
                    }, onFailure = {
                        callback.failedToUpdateUserProfile(it)
                    }
                )
            }, { err ->
                Log.e("UserProfile", "error = ${err.message}")
                callback.failedToUpdateUserProfile(err.getNetworkCallErrorMessage(application))
            })
    }

}