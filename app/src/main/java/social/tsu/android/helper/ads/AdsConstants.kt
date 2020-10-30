package social.tsu.android.helper.ads

import android.util.Log
import social.tsu.android.BuildConfig

class AdsConstants {

    companion object {
        const val LOG_TAG = "FeedAds"

        //MoPub
        val MOPUB_AD_UNIT_ID = "6ab312a0dceb4f419eefab9b2f737f0c"
        //debug:
        //val MOPUB_AD_UNIT_ID = "b195f8dd8ded45fe847ad89ed1d016da"

        val ADCOLONY_APP_ID = "app1e40a05fbb684e1db5"
        val ADCOLONY_ZONE_ID_1 = "vza650090dbe2d428e80"
        //val ADCOLONY_ZONE_ID_2 = ""

        val NATIVE_AD_UNIT_ID = when (BuildConfig.BUILD_TYPE) {
            "debug" -> {
                Log.d(LOG_TAG, "Use Native debug key")
                "/22070274934/test-android-feed"
            }
            else -> "/22070274934/AndroidNative"
        }

        // Interstitial Ads id
        val INTERSTITIAL_AD_UNIT_ID = when (BuildConfig.BUILD_TYPE) {
            "debug" -> {
                Log.d(LOG_TAG, "Use Interstitial debug key")
                "/22070274934/test-android-interstitial"
            }
            else -> "/22070274934/AndroidInterstitial"
        }

        // Exclusive Interstitial Ads id
        val EXCLUSIVE_INTERSTITIAL_AD_UNIT_ID = when (BuildConfig.BUILD_TYPE) {
            "debug" -> {
                Log.d(LOG_TAG, "Use Exclusive Interstitial debug key")
                "/22070274934/test-android-interstitial"
            }
            else -> "/22070274934/android-exclusive-interstitial"
        }

        // Exclusive Rewarded Ads id
        val EXCLUSIVE_REWARDED_AD_UNIT_ID = when (BuildConfig.BUILD_TYPE) {
            "debug" -> {
                Log.d(LOG_TAG, "Use Exclusive Rewarded debug key")
                "/22070274934/test-android-rewarded"
            }
            else -> "/22070274934/android-exclusive-rewarded"
        }

    }
}