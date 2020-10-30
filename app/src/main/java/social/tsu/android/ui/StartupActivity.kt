package social.tsu.android.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.tapjoy.internal.it
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.Constants
import social.tsu.android.network.api.ConfigurationApi
import social.tsu.android.network.model.CreateAccountResponse
import social.tsu.android.network.model.LoginRequest
import social.tsu.android.service.AuthenticationService
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.AppVersion
import javax.inject.Inject

class StartupActivity: AppCompatActivity() {

    private var mImageUrl: Uri? = null
    private var mVideoUrl: Uri? = null

    @Inject
    lateinit var authenticationService: AuthenticationService

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var appConfiguration: ConfigurationApi

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    //Set this to false for now. Needs to be updated via API or launchdarkly in the future
    private var isForceUpdateEnabled: Boolean = false
    companion object {
        const val REQUEST_CODE_IMMEDIATE_UPDATE = 2002
        const val REQUEST_CODE_FLEXIBLE_UPDATE = 2003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.startup_layout)
        (this.application as TsuApplication).appComponent.inject(this)
        this.supportActionBar?.hide()
        fetchConfiguration()

        FirebaseDynamicLinks.getInstance().getDynamicLink(intent).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("INVITATION", it.result?.link.toString())
            }
        }

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                        mImageUrl = it
                    }
                } else if (intent.type?.startsWith("video/") == true) {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                        mVideoUrl = it
                    }
                }
            }
        }
    }

    private fun fetchConfiguration() {
        val subscribe = appConfiguration.getConfiguration()
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        sharedPrefManager.setMinRedeemBalanceValue(result.data.minRedeemValue)
                        sharedPrefManager.setMaxTsupportersPerDayValue(result.data.maxSupportsPerDay)
                        try {
                            val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
                            val version = pInfo.versionName
                            isForceUpdateEnabled =
                                compareVersionNames(result.data.minAndroidVersion, version) == 1
                            Log.e(
                                "versionCheck",
                                "versionCheck :" +
                                        result.data.minAndroidVersion + " " + isForceUpdateEnabled
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            e.printStackTrace()
                        }
                        /**
                         * Call App update Method
                         */
                        updateCheck()
                    },
                    onFailure = { errorMsg ->
                        updateCheck()
                    }
                )

            }, {
                it.printStackTrace()
                updateCheck()
            })

    }

    fun updateCheck(){
        if (isForceUpdateEnabled)
            appImmediateUpdate()
        else
            autoLoginCall()
    }

    fun compareVersionNames(
        oldVersionName: String,
        newVersionName: String
    ): Int {
        var res = 0
        val oldNumbers =
            oldVersionName.split("\\.".toRegex()).toTypedArray()
        val newNumbers =
            newVersionName.split("\\.".toRegex()).toTypedArray()

        // To avoid IndexOutOfBounds
        val maxIndex = Math.min(oldNumbers.size, newNumbers.size)
        for (i in 0 until maxIndex) {
            val oldVersionPart = Integer.valueOf(oldNumbers[i])
            val newVersionPart = Integer.valueOf(newNumbers[i])
            if (oldVersionPart < newVersionPart) {
                res = -1
                break
            } else if (oldVersionPart > newVersionPart) {
                res = 1
                break
            }
        }

        // If versions are the same so far, but they have different length...
        if (res == 0 && oldNumbers.size != newNumbers.size) {
            res = if (oldNumbers.size > newNumbers.size) 1 else -1
        }
        return res
    }
    /***
     * First Call Immediate Update
     */
    private fun appImmediateUpdate() {
        val updateManager = AppUpdateManagerFactory.create(this)
        updateManager.appUpdateInfo.addOnFailureListener {
            /**
             * If immediate update fail then set flexible
             */
            appFlexibleUpdate()
        }
        updateManager.appUpdateInfo.addOnSuccessListener {
            if ((it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
                it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                updateManager.startUpdateFlowForResult(
                    it,
                    AppUpdateType.IMMEDIATE,
                    this,
                    REQUEST_CODE_IMMEDIATE_UPDATE
                )
            } else {
                /**
                 * if  update not available on play store
                 */
                autoLoginCall()
            }
        }
    }

    /**
     * Flexible update
     */
    private fun appFlexibleUpdate() {
        val updateManager = AppUpdateManagerFactory.create(this)
        updateManager.appUpdateInfo.addOnFailureListener {
            /**
             * If immediate update fail then Login
             */
            autoLoginCall()
        }
        updateManager.appUpdateInfo.addOnSuccessListener {
            if ((it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
                it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                updateManager.startUpdateFlowForResult(
                    it,
                    AppUpdateType.FLEXIBLE,
                    this,
                    REQUEST_CODE_FLEXIBLE_UPDATE
                )
            } else {
                /**
                 * if  update not available on play store
                 */
                autoLoginCall()
            }
        }
    }

    private fun autoLoginCall() {
        val loginInfo = AuthenticationHelper.loginInfo(this)
        if (!loginInfo.first.isEmpty() && !loginInfo.second.isEmpty()) {
            logUserIn(loginInfo.first, loginInfo.second)
        } else {
            if (mImageUrl != null || mVideoUrl != null) {
                Toast.makeText(this, getString(R.string.content_error_msg), Toast.LENGTH_LONG)
                    .show()
            }
            navigateToSignUp()
        }
    }

    private fun logUserIn(username: String, password: String) {
        val request = LoginRequest(
            login = username,
            password = password,
            deviceId = "device",
            clientVersion = AppVersion.versionNameCodeConcat
        )
        authenticationService.authenticate(request, this::navigateToSuccess, this::handleError)
    }

    private fun navigateToSignUp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("default_layout", MainActivityDefaultLayout.SIGN_UP)
        startActivity(intent)
    }

    private fun navigateToSuccess(response: CreateAccountResponse) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("default_layout", MainActivityDefaultLayout.FEED)
        mImageUrl?.let {
            intent.putExtra(Constants.IMAGE_URI, it.toString())
        }
        mVideoUrl?.let {
            intent.putExtra(Constants.VIDEO_URI, it.toString())
        }
        startActivity(intent)
    }

    private fun handleError(t: Throwable) {
        when (t.message) {
            getString(R.string.connectivity_issues_message) -> {
                if (!isFinishing) {
                    AlertDialog.Builder(this)
                        .setMessage(t.message)
                        .setPositiveButton(R.string.retry) { dialog, _ ->
                            val loginInfo = AuthenticationHelper.loginInfo(this)
                            logUserIn(loginInfo.first, loginInfo.second)
                            dialog.dismiss()
                        }
                        .show()
                }
            }
            else -> {
                t.message?.let { snack(it) }
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("default_layout", MainActivityDefaultLayout.LOG_IN)
                startActivity(intent)

            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        analyticsHelper.onPreSaveState(outState)
        super.onSaveInstanceState(outState)
        analyticsHelper.onPostSaveState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FLEXIBLE_UPDATE || requestCode == REQUEST_CODE_IMMEDIATE_UPDATE) {
            when (resultCode) {
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    snack(getString(R.string.generic_error_message))
                }
            }
            finish()
        }
    }

}
