package social.tsu.android

import android.app.Application
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.soloader.SoLoader
import okhttp3.OkHttpClient

class FlipperConfig: Flipper {
    private val networkFlipperPlugin =
        NetworkFlipperPlugin()

    override fun addInterceptors(okHttpBuilder: OkHttpClient.Builder) {
        okHttpBuilder.addNetworkInterceptor(
            FlipperOkhttpInterceptor(
                networkFlipperPlugin
            )
        )
    }

    override fun init(application: Application) {
        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(
                application
            )
        ) {
            SoLoader.init(application, false)
            val client =
                AndroidFlipperClient.getInstance(
                    application
                )
            client.addPlugin(
                InspectorFlipperPlugin(
                    application,
                    DescriptorMapping.withDefaults()
                )
            )
            client.addPlugin(networkFlipperPlugin)
            client.start()
        }
    }
}
