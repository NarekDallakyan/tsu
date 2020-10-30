package social.tsu.android.helper.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import social.tsu.android.RxSchedulers
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.helper.DateHelper
import social.tsu.android.service.SharedPrefManager
import javax.inject.Inject


class PublicPostSupportAdsLoader @Inject constructor(
    sharedPrefManager: SharedPrefManager,
    rxSchedulers: RxSchedulers,
    postFeedRepo: PostFeedRepository
) : BasePostSupportAdsLoader(sharedPrefManager, rxSchedulers, postFeedRepo) {

    override val loaderName: String = "Public"

    override fun createInterstitialAd(activity: Activity): PublisherInterstitialAd? = InterstitialAdManager.getInstance(activity).ad

    override fun createRewardedAd(activity: Activity): RewardedAd? = null

    override fun reloadSupportAds() {
        Log.d(tag, "reloadSupportAds")
        if (isDateChanged()) {
            sharedPrefManager.setLastTsupportDateValue(DateHelper.getCurrentDate())
            resetSupportCounter()
            loadInterstitialAd()
        } else {
            if (!isAdLimitReached()) {
                loadInterstitialAd()
            } else {
                disableSupportMeButton()
                Log.d(tag, "Interstitial ad limit per day reached.")
            }
        }
    }

    override fun retry() {
        loadInterstitialAd()
    }

    private fun isAdLimitReached(): Boolean {
        // if lastCounterValue less than limit return true
        return sharedPrefManager.getTsupportsCounterValue() >= sharedPrefManager.getMaxTsupportersPerDayValue()
    }

    private fun isDateChanged(): Boolean {
        val lastSavedDate = sharedPrefManager.getLastTsupportDateValue()
        val currentDate = DateHelper.getCurrentDate()
        return lastSavedDate != currentDate
    }

    private fun resetSupportCounter() {
        sharedPrefManager.setTsupportsCounterValue(0)
    }

}