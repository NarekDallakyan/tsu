package social.tsu.android.helper.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.OnPublisherAdViewLoadedListener
import com.google.android.gms.ads.formats.PublisherAdViewOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import social.tsu.android.BuildConfig
import social.tsu.android.ui.model.AdContent


private const val TAG = "FeedAdsFetcher"

/**
 * AppLovin mediation requires activity context
 */
class GoogleAdFetcher private constructor(private val context: Activity) : AdFetcher {

    private val cache = HashMap<Int, AdCache>()
    private val preloadedAds = ArrayList<AdCache>()
    private var adLoaders = ArrayList<AdLoader>()

    init {
        prepareLoaders()
        preloadNewAds()
    }

    companion object {
        private const val MIN_PRELOADED_ADS = 5
        private const val CACHE_VALID_TIME = 1000 * 60 * 60 // 1 hour

        private val caches = HashMap<String, GoogleAdFetcher>()

        fun getInstance(key: String, activity: Activity): GoogleAdFetcher? {
            if (caches.containsKey(key)) {
                return caches[key]!!
            }

            val nativeAdFetcher = GoogleAdFetcher(activity)
            caches[key] = nativeAdFetcher
            return nativeAdFetcher
        }
    }

    override fun getAd(position: Int): AdContent {
        Log.d(TAG, "Ad requsted at $position")
        if (cache.containsKey(position)) {
            Log.d(TAG, "Cache hit at $position")
            if (!cacheIsValid(position)) {
                Log.d(TAG, "Ad cache is invalid at $position. Destroying...")
                cache[position]!!.ad.bannerAdContent?.destroy()
                cache[position]!!.ad.adContent?.destroy()
                cache.remove(position)
                return getAd(position)
            }
            return cache[position]!!.ad
        } else {
            if (preloadedAds.isNotEmpty()) {
                cache[position] = getNextPreloadedAd()
                return cache[position]!!.ad
            } else {
                preloadNewAds()
                return AdContent()
            }
        }
    }

    override fun onAdRecycled(position: Int) {
        if (cache[position]?.ad?.adContent?.mediaContent?.hasVideoContent() == true) {
            cache[position]?.ad?.adContent?.destroy()
            cache.remove(position)
            Log.d(TAG, "Recycled video ad at $position")
        }
    }

    override fun onDestroy() {
        val videoAds = HashMap<Int, AdCache>()
        cache.filterTo(videoAds, { it.value.ad.adContent?.mediaContent?.hasVideoContent() == true })
        videoAds.forEach {
            it.value.ad.adContent?.destroy()
            cache.remove(it.key)
        }
        Log.d(TAG, "onDestroy() called. Removed ${videoAds.size} video ads")
    }

    private fun cacheIsValid(position: Int): Boolean {
        return adIsValid(cache[position]!!)
    }

    private fun adIsValid(ad: AdCache) : Boolean {
        return (System.currentTimeMillis() - ad.timestamp) < CACHE_VALID_TIME
    }

    private fun preloadNewAds() {
        val numAdsToLoad = MIN_PRELOADED_ADS - preloadedAds.size
        Log.d(TAG, "Preloading new ads count: $numAdsToLoad")
        adLoaders.filter { !it.isLoading }
            .take(numAdsToLoad)
            .forEach { adLoader ->
                Log.d(TAG, "Start loading next")
                adLoader.loadAd(AdRequest.Builder().build())
            }
    }

    private fun prepareLoaders() {
        for (i in 0 until MIN_PRELOADED_ADS) {
            val adLoader = AdLoader.Builder(context, AdsConstants.NATIVE_AD_UNIT_ID)
                .forUnifiedNativeAd { ad: UnifiedNativeAd ->
                    Log.d(TAG, "Unified native ad preloaded #$i")
                    if (BuildConfig.DEBUG) Log.d("${AdsConstants.LOG_TAG}#Provider", "Native ${ad.responseInfo?.mediationAdapterClassName}")
                    preloadedAds.add(AdCache(System.currentTimeMillis(), AdContent(ad)))
                }
                .forPublisherAdView(OnPublisherAdViewLoadedListener { ad ->
                    Log.d(TAG, "Banner ad preloaded #$i")
                    if (BuildConfig.DEBUG) Log.d("${AdsConstants.LOG_TAG}#Provider", "Banner ${ad.responseInfo?.mediationAdapterClassName}")
                    preloadedAds.add(AdCache(System.currentTimeMillis(), AdContent(bannerAdContent = ad)))
                }, AdSize.MEDIUM_RECTANGLE)
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(errorCode: Int) {
                        Log.e(TAG, "Error load ad $errorCode for  #$i")
                    }
                })
                .withPublisherAdViewOptions(PublisherAdViewOptions.Builder().build())
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
            adLoaders.add(adLoader)
        }
    }

    private fun getNextPreloadedAd(): AdCache {
        Log.d(TAG, "Getting preloaded cache")
        checkPreloadedAdsForValidity()
        if (preloadedAds.isNotEmpty()) {
            val first = preloadedAds.first()
            preloadedAds.removeAt(0)
            Log.d(TAG, "Retrieved preloaded ad")
            if (preloadedAds.size < MIN_PRELOADED_ADS) {
                preloadNewAds()
            }
            return first
        } else {
            Log.d(TAG, "No preloading ads. Preloading new ads. Returning empty ad.")
            preloadNewAds()
            return AdCache(System.currentTimeMillis(), AdContent())
        }
    }

    private fun checkPreloadedAdsForValidity() {
        val invalidAds = ArrayList<AdCache>()
        preloadedAds.filterTo(invalidAds, { !adIsValid(it) })
        Log.d(TAG, "Found ${invalidAds.size} invalid ads. Removing them...")
        preloadedAds.removeAll(invalidAds)
    }
}

interface AdFetcher {
    fun getAd(position: Int): AdContent
    fun onAdRecycled(position: Int)
    fun onDestroy()
}

data class AdCache(
    val timestamp: Long,
    val ad: AdContent
)