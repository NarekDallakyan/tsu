package social.tsu.android.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.ads.ExclusiveAdsPostSupportLoader
import social.tsu.android.helper.ads.PublicPostSupportAdsLoader
import javax.inject.Inject


class AdsSupportViewModel @Inject constructor(
    private val publicPostSupportAdsLoader: PublicPostSupportAdsLoader,
    private val exclusiveAdsPostSupportLoader: ExclusiveAdsPostSupportLoader
) : ViewModel() {

    val publicSupportEnabledLiveData = publicPostSupportAdsLoader.supportEnabledLiveData
    val exclusiveSupportEnabledLiveData = exclusiveAdsPostSupportLoader.supportEnabledLiveData

    override fun onCleared() {
        super.onCleared()
        publicPostSupportAdsLoader.stopLoading()
        exclusiveAdsPostSupportLoader.stopLoading()
    }

    fun initAds(activity: Activity) {
        publicPostSupportAdsLoader.initAds(activity)
        exclusiveAdsPostSupportLoader.initAds(activity)
    }

    fun reloadSupportAds() {
        publicPostSupportAdsLoader.reloadSupportAds()
        exclusiveAdsPostSupportLoader.reloadSupportAds()
    }

    fun didTapSupportButton(activity: Activity, post: Post, onLoad: () -> Unit) {
        if (post.privacy == 2) {
            exclusiveAdsPostSupportLoader.supportPost(activity, post, onLoad)
        } else {
            publicPostSupportAdsLoader.supportPost(activity, post, onLoad)
        }
    }

}