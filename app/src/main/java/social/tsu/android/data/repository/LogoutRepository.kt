package social.tsu.android.data.repository

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.bumptech.glide.Glide
import social.tsu.android.data.local.LocalDatabase
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.service.LogoutService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.TsuNotificationsService
import java.util.concurrent.Executors
import javax.inject.Inject

class LogoutRepository @Inject constructor(
    private val application: Application,
    private val logoutService: LogoutService,
    private val encryptedSharedPrefs: SharedPreferences,
    private val localDatabase: LocalDatabase
) {

    fun logoutUser(){

        logoutService.logout(
            object : ServiceCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    Log.i("LogoutRepository", "Logout user success")
                }

                override fun onFailure(errMsg: String) {
                    Log.w("LogoutRepository", "Logout user failed with error: $errMsg")
                }
            })

        //reset auth tokens and userid
        AuthenticationHelper.clear()
        TsuNotificationsService.unseenNotificationsLiveData.postValue(0)

        //remove persisted login details
        encryptedSharedPrefs.edit()?.apply {
            remove("LOGIN_USER")
            remove("LOGIN_PASS")
            apply()
        }

        Executors.newSingleThreadExecutor().execute {
            localDatabase.clearAllTables()
            Glide.get(application).clearDiskCache()
        }

    }

}