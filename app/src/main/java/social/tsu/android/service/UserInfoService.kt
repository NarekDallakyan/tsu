package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.UserInfoCache
import social.tsu.android.network.api.UserApi
import social.tsu.android.network.api.UserSettingsApi
import social.tsu.android.network.model.AccountInfoDTO
import social.tsu.android.network.model.AccountInfoUser
import social.tsu.android.network.model.UserProfile
import social.tsu.android.rx.plusAssign
import java.net.UnknownHostException
import javax.inject.Inject

interface UserInfoServiceCallback: DefaultServiceCallback {
    fun completedGetUserInfo(info: UserProfile?)
    fun completedUpdateUserAccount(info: UserProfile?) {}
}

interface UserAccountDeleteCallback {
    fun completeUserAccountDelete(boolean: Boolean)
    fun failedUserAccountDelete(message: String)
}

abstract class UserInfoService: DefaultService() {

    abstract var callback: UserInfoServiceCallback?

    abstract fun getUserInfo(userId: Int, ignoringCache: Boolean)

    abstract fun getUserTagInfo(userTag:String , ignoringCache: Boolean)

    abstract fun cacheUserInfo(userId: Int)

    abstract fun getCachedUserInfo(userId: Int): UserProfile?

    abstract fun updateAccountInfo(user: AccountInfoUser)

    abstract fun deleteAccount(userId: Int, password: String)

}

class DefaultUserInfoService(
    private val application: TsuApplication,
    callback: UserInfoServiceCallback
): UserInfoService() {

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var userApi: UserApi

    @Inject
    lateinit var userSettingsApi: UserSettingsApi

    var userAccountDeleteCallback: UserAccountDeleteCallback? = null

    constructor(
        application: TsuApplication,
        callback: UserInfoServiceCallback,
        deleteCallback: UserAccountDeleteCallback
    ) : this(application, callback) {
        this.userAccountDeleteCallback = deleteCallback
    }

    private val profileImageService: DefaultUserProfileImageService by lazy {
        DefaultUserProfileImageService(application)
    }

    override var callback: UserInfoServiceCallback? = callback

    override val compositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultUserInfoService"

    private val cache: UserInfoCache = UserInfoCache

    init {
        application.appComponent.inject(this)
    }

    override fun updateAccountInfo(user: AccountInfoUser) {
        compositeDisposable += userSettingsApi.updateAccountInfo(AccountInfoDTO(user))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        val info = result.data
                        AuthenticationHelper.update(info)
                        cache[info.id] = info
                        callback?.completedUpdateUserAccount(info)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                Log.e("UserProfile", "error = ${err.message}")
                callback?.completedUpdateUserAccount(null)
            })
    }

    override fun deleteAccount(userId: Int, password: String) {


        compositeDisposable += userSettingsApi.deleteAccount(userId, password)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        // nothing here
                    },
                    onFailure = { errMsg ->

                        if (response.code() == 204) {
                            userAccountDeleteCallback?.completeUserAccountDelete(true)
                        } else {
                            userAccountDeleteCallback?.failedUserAccountDelete(errMsg)
                        }
                    }
                )
            }, { err ->
                userAccountDeleteCallback?.failedUserAccountDelete("" + err.printStackTrace())
            })
    }

    override fun getCachedUserInfo(userId: Int): UserProfile? {
        return cache[userId]
    }

    override fun getUserInfo(userId: Int, ignoringCache: Boolean) {
        if (userId < 0) {
            callback?.completedGetUserInfo(null)
            return
        }

        val info = cache[userId]
        if (!ignoringCache && info != null) {
            callback?.completedGetUserInfo(info)
            return
        }

        compositeDisposable += userApi.getUserInfo(userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        val profile = result.data
                        Log.d(
                            "UserProfile",
                            "fetchUserInfo before update friend status: ${profile.friendshipStatus}"
                        )
                        cache[userId] = profile
                        profileImageService.getCoverPicture(profile.coverPictureUrl, true, null)
                        profileImageService.getProfilePicture(profile.profilePictureUrl, true, null)
                        callback?.completedGetUserInfo(profile)
                    },
                    onFailure = {
                        callback?.completedGetUserInfo(null)
                        callback?.didErrorWith(it)
                    }
                )
            }, { err ->
                Log.e("UserProfile", "error = ${err.message}")
                if (err is UnknownHostException)
                    callback?.didErrorWith(application.getString(R.string.connectivity_issues_message))
                else
                    callback?.didErrorWith(err.message ?: "")
            })
    }

    override fun getUserTagInfo(userTag: String, ignoringCache: Boolean) {

        compositeDisposable += userApi.getTagUserInfo(userTag)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        val profile = result.data
                        Log.d(
                            "UserProfile",
                            "fetchUserInfo before update friend status: ${profile.friendshipStatus}"
                        )
                        //cache[userId] = profile
                        profileImageService.getCoverPicture(profile.coverPictureUrl, true, null)
                        profileImageService.getProfilePicture(profile.profilePictureUrl, true, null)
                        callback?.completedGetUserInfo(profile)
                    },
                    onFailure = {
                        callback?.completedGetUserInfo(null)
                        callback?.didErrorWith(it)
                    }
                )
            }, { err ->
                Log.e("UserProfile", "error = ${err.message}")
                if (err is UnknownHostException)
                    callback?.didErrorWith(application.getString(R.string.connectivity_issues_message))
                else
                    callback?.didErrorWith(err.message ?: "")            })

    }

    override fun cacheUserInfo(userId: Int) {
        if (userId < 0) {
            return
        }

        compositeDisposable += userApi.getUserInfo(userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        val profile = result.data
                        Log.d(
                            "UserProfile",
                            "fetchUserInfo before update friend status: ${profile.friendshipStatus}"
                        )
                        cache[userId] = profile
                        profileImageService.getCoverPicture(profile.coverPictureUrl, true, null)
                        profileImageService.getProfilePicture(profile.profilePictureUrl, true, null)
                        profile.relationshipWithId?.let {
                            getRelationshipUserInfo(it)
                        }
                    }
                )
            }, { err ->
                Log.e("UserProfile", "error = ${err.message}")
            })
    }

    private fun getRelationshipUserInfo(relationshipId: Int) {
        if (cache[relationshipId] != null) {
            return
        }
        cacheUserInfo(relationshipId)
    }

}