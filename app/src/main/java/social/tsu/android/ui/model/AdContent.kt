package social.tsu.android.ui.model

import com.google.android.gms.ads.doubleclick.PublisherAdView
import com.google.android.gms.ads.formats.UnifiedNativeAd

class AdContent(var adContent: UnifiedNativeAd? = null, var bannerAdContent: PublisherAdView? = null) :
    FeedContent<Boolean> {

    override fun getContent(): Boolean {
        return true
    }
}
