package social.tsu.android.helper.ads

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.ads.mediation.tapjoy.TapjoyAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import social.tsu.android.BuildConfig
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.entity.Post
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.service.SharedPrefManager
import java.util.concurrent.TimeUnit


private const val AD_TAG = "SupportAds"

abstract class BasePostSupportAdsLoader(
    protected val sharedPrefManager: SharedPrefManager,
    private val rxSchedulers: RxSchedulers,
    private val postFeedRepo: PostFeedRepository
) {

    private val _supportEnabledLiveData = MutableLiveData<Boolean>()
    val supportEnabledLiveData: LiveData<Boolean> = _supportEnabledLiveData
    protected var activity: Activity? = null

    protected var interstitialDisposable: Disposable? = null
    protected var interstitialRetryTimer = 1000L

    protected lateinit var supportedPost: Post
    protected var interstitialAd: PublisherInterstitialAd? = null
    protected var rewardedAd: RewardedAd? = null

    //rewardedad has no loading state flag
    private var rewardedAdLoading = false

    fun initAds(activity: Activity) {
        this.activity = activity
        interstitialAd = createInterstitialAd(activity)
        rewardedAd = createRewardedAd(activity)

        Log.d(tag, "Inited")
    }

    protected abstract val loaderName: String

    abstract fun createInterstitialAd(activity: Activity): PublisherInterstitialAd?

    abstract fun createRewardedAd(activity: Activity): RewardedAd?

    abstract fun reloadSupportAds()

    internal abstract fun retry()

    fun stopLoading() {
        interstitialDisposable?.dispose()
    }

    protected val tag: String by lazy { "$AD_TAG#$loaderName" }

    private val rewardedAdLoadListener = object : RewardedAdLoadCallback() {
        override fun onRewardedAdFailedToLoad(error: LoadAdError) {
            super.onRewardedAdFailedToLoad(error)
            rewardedAdLoading = false

            logAdError(error)

            //fallback to interstitial
            loadInterstitialAd()
        }

        override fun onRewardedAdLoaded() {
            super.onRewardedAdLoaded()
            rewardedAdLoading = false

            Log.d(tag, "RewardedAd is loaded")
            if (BuildConfig.DEBUG) {
                Log.d(tag, "Rewarded - ${rewardedAd?.mediationAdapterClassName}")
            }
            interstitialRetryTimer = 1000
            enableSupportMeButton()
        }

    }

    private fun logAdError(error: LoadAdError) {
        val errorDomain = error.domain
        Log.d(tag, "errorDomain: $errorDomain")
        // Gets the error code. See
        // https://developers.google.com/android/reference/com/google/android/gms/ads/AdRequest#constant-summary
        // for a list of possible codes.
        val errorCode = error.code
        Log.d(tag, "errorCode: $errorCode")
        // Gets an error message.
        // For example "Account not approved yet". See
        // https://support.google.com/admob/answer/9905175 for explanations of
        // common errors.
        val errorMessage = error.message
        Log.d(tag, "errorMessage: $errorMessage")
        // Gets the cause of the error, if available.
        val cause = error.cause
        Log.d(tag, "cause: $cause")
    }


    private val rewardedAdListener = object : RewardedAdCallback() {
        override fun onUserEarnedReward(p0: com.google.android.gms.ads.rewarded.RewardItem) {
        }

        override fun onRewardedAdClosed() {
            super.onRewardedAdClosed()
            Log.d(tag, "RewardedAd closed")
            disableSupportMeButton()
            //create another instance of
            activity?.let {
                rewardedAd = createRewardedAd(it)
                Log.d(tag, "Next RewardedAd: ${rewardedAd?.responseInfo?.mediationAdapterClassName}")
            }
        }

        override fun onRewardedAdOpened() {
            super.onRewardedAdOpened()
            Log.d(tag, "RewardedAd opened | post_id:${supportedPost.originalPostId}")
            postFeedRepo.supportPost(supportedPost.originalPostId.toLong())
        }
    }

    private val adListener = object : AdListener() {
        override fun onAdLoaded() {
            // Code to be executed when an ad finishes loading.
            Log.d(tag, "InterstitialAd loaded")
            if (BuildConfig.DEBUG) {
                Log.d(tag, "Interstitial ${interstitialAd?.mediationAdapterClassName}")
            }
            interstitialRetryTimer = 1000
            enableSupportMeButton()
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            logAdError(error)

            interstitialRetryTimer *= 2
            interstitialDisposable?.dispose()
            interstitialDisposable = Single.timer(interstitialRetryTimer, TimeUnit.MILLISECONDS)
                .subscribeOn(rxSchedulers.io())
                .observeOn(rxSchedulers.main())
                .subscribe { _ -> retry() }
        }

        override fun onAdOpened() {
            // Code to be executed when the ad is displayed.
            Log.d(tag, "InterstitialAd opened | post_id:${supportedPost.originalPostId}")
            postFeedRepo.supportPost(supportedPost.originalPostId.toLong())
        }

        override fun onAdClicked() {
            // Code to be executed when the user clicks on an ad.
        }

        override fun onAdLeftApplication() {
            // Code to be executed when the user has left the app.
        }

        override fun onAdClosed() {
            // Code to be executed when the interstitial ad is closed.
            Log.d(tag, "InterstitialAd closed")
            disableSupportMeButton()
        }
    }

    open fun supportPost(activity: Activity, post: Post, onSupported: () -> Unit) {
        val interstitialAd = this.interstitialAd
        val rewardedAd = this.rewardedAd
        supportedPost = post

        if (rewardedAd != null && rewardedAd.isLoaded) {
            onPostSupported(post, onSupported)
            rewardedAd.show(activity, rewardedAdListener)
            Log.d(tag, "RewardedAd is showed")
        } else if (interstitialAd != null && interstitialAd.isLoaded) {
            onPostSupported(post, onSupported)
            interstitialAd.show()
            Log.d(tag, "InterstitialAd is showed")
        } else {
            Toast.makeText(activity, "The interstitial wasn't loaded yet.", Toast.LENGTH_SHORT)
                .show()
            Log.d(tag, "The interstitial wasn't loaded yet")
        }
    }

    private fun onPostSupported(post: Post, onReadyToShow: () -> Unit) {
        incrementSupportCounter()
        sharedPrefManager.getSupportPostId()?.let {
            val supportPostIds = ArrayList<String>()
            supportPostIds.addAll(it.split(","))
            supportPostIds.add(post.id.toString())
            sharedPrefManager.setSupportPostId(TextUtils.join(",", supportPostIds))
            onReadyToShow()
        }
    }

    internal fun loadRewardedAd() {
        val rewardedAd = this.rewardedAd ?: return

        if (!rewardedAd.isLoaded) {
            Log.d(tag, "RewardedAd is not loaded. Request new ad")
            rewardedAd.loadAd(buildRequest(), rewardedAdLoadListener)
        } else if (rewardedAdLoading) {
            Log.d(tag, "RewardedAd is loading")
            disableSupportMeButton()
        } else {
            Log.d(tag, "RewardedAd loaded and can be shown")
            enableSupportMeButton()
        }
    }

    internal fun loadInterstitialAd() {
        val interstitialAd = this.interstitialAd ?: return

        interstitialAd.adListener = adListener
        if (!interstitialAd.isLoaded && !interstitialAd.isLoading) {
            Log.d(tag, "InterstitialAd is not loaded. Request new ad")
            interstitialAd.loadAd(buildRequest())
        } else if (interstitialAd.isLoading) {
            Log.d(tag, "InterstitialAd is loading")
            disableSupportMeButton()
        } else {
            Log.d(tag, "InterstitialAd loaded and can be shown")
            enableSupportMeButton()
        }
    }

    private fun buildRequest(): PublisherAdRequest {
        val tapjoyExtras = TapjoyAdapter.TapjoyExtrasBundleBuilder()
            .setDebug(BuildConfig.DEBUG)
            .build()
        return PublisherAdRequest.Builder()
            .addNetworkExtrasBundle(TapjoyAdapter::class.java, tapjoyExtras)
            .build()
    }

    internal fun disableSupportMeButton() {
        _supportEnabledLiveData.postValue(false)
    }

    internal fun enableSupportMeButton() {
        _supportEnabledLiveData.postValue(true)
    }

    private fun incrementSupportCounter() {
        val currentCounterValue = sharedPrefManager.getTsupportsCounterValue()
        sharedPrefManager.setTsupportsCounterValue(currentCounterValue + 1)
    }

}