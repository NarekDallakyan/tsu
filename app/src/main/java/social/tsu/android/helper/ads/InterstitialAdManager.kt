package social.tsu.android.helper.ads

import android.content.Context
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import social.tsu.android.helper.SingletonHolder

class InterstitialAdManager private constructor(context: Context){
    companion object : SingletonHolder<InterstitialAdManager, Context>(::InterstitialAdManager)
    val ad: PublisherInterstitialAd = PublisherInterstitialAd(context)
    val exclusiveAd: PublisherInterstitialAd = PublisherInterstitialAd(context)

    init {

        ad.adUnitId = AdsConstants.INTERSTITIAL_AD_UNIT_ID
        exclusiveAd.adUnitId = AdsConstants.EXCLUSIVE_INTERSTITIAL_AD_UNIT_ID
    }
}