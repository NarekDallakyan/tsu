package social.tsu.android.helper.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import social.tsu.android.RxSchedulers
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.service.SharedPrefManager
import javax.inject.Inject


class ExclusiveAdsPostSupportLoader @Inject constructor(
    application: Application,
    sharedPrefManager: SharedPrefManager,
    rxSchedulers: RxSchedulers,
    postFeedRepo: PostFeedRepository
) : BasePostSupportAdsLoader(sharedPrefManager, rxSchedulers, postFeedRepo) {

    override val loaderName: String = "Exclusive"

    override fun createInterstitialAd(activity: Activity): PublisherInterstitialAd? =
        InterstitialAdManager.getInstance(activity).exclusiveAd

    override fun createRewardedAd(activity: Activity): RewardedAd? =
        RewardedAd(activity, AdsConstants.EXCLUSIVE_REWARDED_AD_UNIT_ID)

    override fun reloadSupportAds() {
        Log.d(tag, "reloadSupportAds")
        loadRewardedAd()
    }

    override fun retry() {
        loadRewardedAd()
    }

}