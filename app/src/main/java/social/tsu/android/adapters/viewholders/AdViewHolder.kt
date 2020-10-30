package social.tsu.android.adapters.viewholders

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.ablanco.zoomy.Zoomy
import com.google.android.gms.ads.formats.MediaView
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.formats.UnifiedNativeAdView
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.model.AdContent
import social.tsu.android.utils.hide
import social.tsu.android.utils.show

class AdViewHolder(
    itemView: View,
    private val application: TsuApplication,
    private val parent: ViewGroup
) : PostViewHolder(application, null, itemView) {

    private val divider: View? = itemView.findViewById(R.id.divider)
    private val unifiedNativeAdView: UnifiedNativeAdView = itemView.findViewById(R.id.native_ad)
    private val bannerViewFrame: FrameLayout = itemView.findViewById(R.id.banner_ad_frame)

    fun bind(ad: AdContent) {
        divider.show()
        ad.adContent?.let {
            unifiedNativeAdView.show()
            bannerViewFrame.hide()
            populateUnifiedNativeAdView(it, unifiedNativeAdView)
        } ?: run {
            unifiedNativeAdView.hide()
            Log.d("ADS", "no native ad")
            ad.bannerAdContent?.let {
                Log.d("ADS", "showing banner view")
                if (bannerViewFrame.childCount > 0) {
                    bannerViewFrame.removeAllViews()
                }
                if (it.parent != null) {
                    (it.parent as ViewGroup).removeView(it)
                }
                bannerViewFrame.addView(it)
                val layoutParams = it.layoutParams as FrameLayout.LayoutParams
                val margin = application.resources.getDimension(R.dimen.standard_margin).toInt()
                layoutParams.setMargins(0, margin, 0, margin)
                it.layoutParams = layoutParams
                bannerViewFrame.show()
            } ?: run {
                Log.d("ADS", "no banner ad")
                bannerViewFrame.hide()
                divider.hide()
            }
        }
    }

    private fun populateUnifiedNativeAdView(
        nativeAd: UnifiedNativeAd,
        adView: UnifiedNativeAdView
    ) {
        // You must call destroy on old ads when you are done with them,
        // otherwise you will have a memory leak.
//        currentNativeAd?.destroy()
//        currentNativeAd = nativeAd
        // Set the media view.
        adView.mediaView = adView.findViewById<MediaView>(
            R.id.ad_media
        )

        val adViewImage = adView.findViewById<AppCompatImageView>(R.id.ad_media_image)
        val builder: Zoomy.Builder = Zoomy.Builder(MainActivity.instance)
            .target(adViewImage)
            .interpolator(OvershootInterpolator())

        builder.register()
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        if (nativeAd.mediaContent.hasVideoContent()) {
            adViewImage.hide()
            adView.mediaView.setMediaContent(nativeAd.mediaContent)
            adView.mediaView.show()
        } else {
            adView.mediaView.hide()
            adViewImage.setImageDrawable(nativeAd.mediaContent.mainImage)
            adViewImage.show()
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView.visibility = View.INVISIBLE
        } else {
            adView.bodyView.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView.visibility = View.INVISIBLE
        } else {
            adView.callToActionView.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon.drawable)
            adView.iconView.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            adView.priceView.visibility = View.INVISIBLE
        } else {
            adView.priceView.visibility = View.VISIBLE
            (adView.priceView as TextView).text =
                if (nativeAd.price.isEmpty()) application.getText(R.string.ad_price_free) else nativeAd.price

        }

        if (nativeAd.store == null) {
            adView.storeView.visibility = View.INVISIBLE
        } else {
            adView.storeView.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }

        //Star rating. Setting it to GONE for now.

        /*if (nativeAd.starRating == null) {
            adView.starRatingView.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView.visibility = View.VISIBLE
        }*/

        adView.starRatingView.visibility = View.GONE

        if (nativeAd.advertiser == null) {
            adView.advertiserView.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView.visibility = View.VISIBLE
        }

        adView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
//        val vc = nativeAd.videoController
//        if (vc.hasVideoContent()) {
//            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
//                override fun onVideoEnd() {
//                    super.onVideoEnd()
//                }
//            }
//        }
//
    }

}
