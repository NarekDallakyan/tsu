package social.tsu.android.helper

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.mopub.common.MoPub
import social.tsu.android.BuildConfig


class ConsentHelper {

    companion object {

        private const val TAG = "ConsentHelper"

        private val _shouldShowConsent = MutableLiveData<Boolean>()
        val shouldShowConsent: LiveData<Boolean> = _shouldShowConsent

        fun getConsent(activity: Activity, reset : Boolean = false) {
            Log.d(TAG, "Getting consent")
            val params = buildConsentParams(activity)
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            if (reset) {
                consentInformation.reset()
            }
            consentInformation.requestConsentInfoUpdate(activity, params, {
                if (consentInformation.isConsentFormAvailable) {
                    Log.d(TAG, "Consent status ${consentInformation.consentStatus}")
                    if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                        loadForm(activity)
                    }
                } else {
                    Log.d(TAG, "No consent form")
                }
            }, { formError ->
                Log.d(TAG, formError.message)
            })
        }

        private fun buildConsentParams(activity: Activity): ConsentRequestParameters? {
            val builder = ConsentDebugSettings.Builder(activity)
            if (BuildConfig.BUILD_TYPE == "debug") {
                builder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
                Log.d(TAG, "Set debug consent geo")
            }
            val params = ConsentRequestParameters.Builder()
                .setConsentDebugSettings(builder.build())
                .build()
            return params
        }

        private fun loadForm(activity: Activity) {
            UserMessagingPlatform.loadConsentForm(activity,
                { consentForm ->
                    consentForm.show(activity) {
                        val personalInformationManager = MoPub.getPersonalInformationManager()
                        val consentInformation =
                            UserMessagingPlatform.getConsentInformation(activity)
                        when(consentInformation.consentType) {
                            ConsentInformation.ConsentType.PERSONALIZED -> personalInformationManager?.grantConsent()
                            ConsentInformation.ConsentType.NON_PERSONALIZED -> personalInformationManager?.revokeConsent()
                        }
                        Log.d(TAG, "Dismissed")
                    }
                }, { formError ->
                    Log.d(TAG, formError.message)
                })
        }

        fun shouldShowConsent(activity: Activity, consentResult: ConsentResult) {
            val params = buildConsentParams(activity)
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.UNKNOWN) {
                consentInformation.requestConsentInfoUpdate(activity, params, {
                    val result = consentInformation.consentStatus != ConsentInformation.ConsentStatus.NOT_REQUIRED
                    consentResult.onConsentResult(result)
                    _shouldShowConsent.postValue(result)
                }, { formError ->
                    Log.d(TAG, formError.message)
                    consentResult.onConsentResult(false)
                    _shouldShowConsent.postValue(false)
                })
            } else {
                val result = consentInformation.consentStatus != ConsentInformation.ConsentStatus.NOT_REQUIRED
                consentResult.onConsentResult(result)
                _shouldShowConsent.postValue(result)
            }
        }

        interface ConsentResult {
            fun onConsentResult(shouldShowConsentButton: Boolean)
        }
    }
}