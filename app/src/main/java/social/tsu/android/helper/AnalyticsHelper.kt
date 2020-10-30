package social.tsu.android.helper

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.indicative.client.android.Indicative
import social.tsu.android.BuildConfig
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.ui.util.BundleUtils

class AnalyticsHelper (application: Application, var sharedPrefManager: SharedPrefManager) : LifecycleObserver {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(application)
    private val firebaseCrashlytics = FirebaseCrashlytics.getInstance()

    companion object {
        private const val EVENT_BACKGROUND = "move_to_background"
        private const val EVENT_FOREGROUND = "move_to_foreground"
        private const val EVENT_LOW_MEMORY = "low_memory"
        private const val EVENT_TRIM_MEMORY = "trim_memory"
        private const val EVENT_PRE_NAV_SAVE_STATE = "pre_nav_save_state"
        private const val EVENT_PRE_SAVE_STATE = "pre_save_state"
        private const val EVENT_POST_NAV_SAVE_STATE = "post_nav_save_state"
        private const val EVENT_POST_SAVE_STATE = "post_save_state"
        private const val EVENT_CALLBACK_CALL = "callback_call"

        private const val ARGUMENT_BUNDLE_SIZE = "bundle_size"
        private const val ARGUMENT_CALLBACK_NAME = "callback_name"
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun setCurrentScreen(activity: Activity, screenName: String) {
        firebaseAnalytics.setCurrentScreen(activity, screenName, null)
    }

    fun log(message: String) {
        firebaseCrashlytics.log(message)
    }

    fun recordException(throwable: Throwable) {
        firebaseCrashlytics.recordException(throwable)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        firebaseAnalytics.logEvent(EVENT_FOREGROUND, null)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        firebaseAnalytics.logEvent(EVENT_BACKGROUND, null)
    }

    fun onLowMemory() {
        firebaseAnalytics.logEvent(EVENT_LOW_MEMORY, null)
    }

    fun onTrimMemory() {
        firebaseAnalytics.logEvent(EVENT_TRIM_MEMORY, null)
    }

    fun onPreSaveNavControllerState() {
        firebaseAnalytics.logEvent(EVENT_PRE_NAV_SAVE_STATE, null)
    }

    fun onPostSaveNavControllerState() {
        firebaseAnalytics.logEvent(EVENT_POST_NAV_SAVE_STATE, null)
    }

    fun onCallbackCalled(name: String) {
        val arguments = Bundle()
        arguments.putString(ARGUMENT_CALLBACK_NAME, name)
        firebaseAnalytics.logEvent(EVENT_CALLBACK_CALL, arguments)
    }

    fun onPreSaveState(state: Bundle?) {
        val arguments = Bundle()
        arguments.putInt(ARGUMENT_BUNDLE_SIZE, BundleUtils.getBundleSizeInBytes(state))
        firebaseAnalytics.logEvent(EVENT_PRE_SAVE_STATE, arguments)
    }

    fun onPostSaveState(state: Bundle?) {
        val arguments = Bundle()
        arguments.putInt(ARGUMENT_BUNDLE_SIZE, BundleUtils.getBundleSizeInBytes(state))
        firebaseAnalytics.logEvent(EVENT_POST_SAVE_STATE, arguments)
    }

    fun logEvent(event: String?,properties:HashMap<String, Any?>) {
        properties["Age"] = sharedPrefManager.getAge()
        properties["timeStamp"] = System.currentTimeMillis() / 1000
        properties["Device Model"] = Build.MANUFACTURER + " " + Build.MODEL
        properties["Device OS"] = "Android"
        properties["Device OS Version"] = Build.VERSION.SDK_INT
        properties["App Version"] = BuildConfig.VERSION_NAME
        properties["App Build"] = BuildConfig.VERSION_CODE
        properties["userId"] = sharedPrefManager.getUserId()
        properties["account_created_at"] = sharedPrefManager.getCreatedAt()
        Indicative.recordEvent(event,properties,true)
    }

    fun logEvent(event: String?) {
        val properties = HashMap<String, Any?>()
        properties["Age"] = sharedPrefManager.getAge()
        properties["timeStamp"] = System.currentTimeMillis() / 1000
        properties["Device Model"] = Build.MANUFACTURER + " " + Build.MODEL
        properties["Device OS"] = "Android"
        properties["Device OS Version"] = Build.VERSION.SDK_INT
        properties["App Version"] = BuildConfig.VERSION_NAME
        properties["App Build"] = BuildConfig.VERSION_CODE
        properties["userId"] = sharedPrefManager.getUserId()
        properties["account_created_at"] = sharedPrefManager.getCreatedAt()
        Indicative.recordEvent(event,properties,true)
    }
}