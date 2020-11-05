package social.tsu.android

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.indicative.client.android.Indicative
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import jp.co.cyberagent.android.gpuimage.GPUImage
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.di.AppComponent
import social.tsu.android.di.DaggerAppComponent
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.network.api.Environment
import social.tsu.android.network.api.ProjectEnvironment
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.ui.post.model.FilterVideoModel
import social.tsu.android.ui.post.view.filter.GPUImageFilterTools
import social.tsu.android.utils.AppVersion
import social.tsu.android.workmanager.DaggerWorkerFactory
import social.tsu.camerarecorder.widget.Filters
import social.tsu.trimmer.widget.VideoTrimmerView
import java.text.SimpleDateFormat
import javax.inject.Inject

open class TsuApplication : Application(),HasAndroidInjector,   CameraXConfig.Provider{

    @Inject
    lateinit var flipper: Flipper

    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    @Inject
    lateinit var workerFactory: DaggerWorkerFactory

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    @Inject
    lateinit var postFeedRepository: PostFeedRepository


    companion object {
        lateinit var mContext: Context

        private fun createBitmap(filter: GPUImageFilterTools.FilterType?): Bitmap? {

            val originalBitmap = BitmapFactory.decodeResource(
                this.mContext.resources, R.drawable.video_trim_girl
            )

            if (filter == null) {

                return originalBitmap
            }
            val gpuImage = GPUImage(this.mContext)
            gpuImage.setImage(originalBitmap)
            gpuImage.setFilter(GPUImageFilterTools.createFilterForType(this.mContext, filter))

            return gpuImage.bitmapWithFilterApplied
        }

        val filterItems: ArrayList<FilterVideoModel> by lazy {

            val filterItems = ArrayList<FilterVideoModel>()
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.NORMAL, bitmaps =
                    createBitmap(null)
                )
            );
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.BILATERAL, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.BILATERAL_BLUR)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.BULGE_DISTORTION, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.BULGE_DISTORTION)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.CGA_COLOR_SPACE, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.CGA_COLORSPACE)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.GAUSSIAN_BLUR, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.GAUSSIAN_BLUR)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.GLAY_SCALE, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.GRAYSCALE)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.INVERT, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.INVERT)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.SEPIA, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.SEPIA)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.SHARPEN, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.SHARPEN)
                )
            )
            filterItems.add(
                FilterVideoModel(
                    R.drawable.cover, Filters.WEAKPIXELINCLUSION, bitmaps =
                    createBitmap(GPUImageFilterTools.FilterType.WEAK_PIXEL_INCLUSION)
                )
            )
            return@lazy filterItems
        }
    }

    override fun onCreate() {
        super.onCreate()
        VideoTrimmerView.initBaseUtils(this)
        mContext = this
        AppVersion.init(this)
        setNetworkEnviorement()
        appComponent.inject(this)
        flipper.init(this)
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        initWorkManager()
        AndroidThreeTen.init(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        //remove stale posts on restart so fresh posts can be fetched for feed
        postFeedRepository.removeCachePosts()
        if (sharedPrefManager.getLaunchTime().isNullOrEmpty()) {
            sharedPrefManager.setLaunchTime(SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis()))
        }
        initAds()
        Indicative.launch(applicationContext, BuildConfig.INDICATIVE_API_KEY);

        val loadLazy = filterItems
    }

    private fun initAds() {
        MobileAds.initialize(this) {
            Log.i("ADS", "init done")
        }

        //Mopub initialization moved to MainActivity
    }

    private fun initWorkManager() {
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        analyticsHelper.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        analyticsHelper.onTrimMemory()
    }

    val appComponent: AppComponent by lazy {
        initalizeComponent()
    }

    open fun initalizeComponent(): AppComponent {
        return DaggerAppComponent.factory().create(applicationContext)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()

    }

    private fun setNetworkEnviorement() {
        if (BuildConfig.BUILD_TYPE == "debug") {
            Environment.set(ProjectEnvironment.test)
        } else if (BuildConfig.BUILD_TYPE == "release") {
            Environment.set(ProjectEnvironment.prd)
        } else if (BuildConfig.BUILD_TYPE == "local") {
            Environment.set(ProjectEnvironment.local)
        }
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}

fun Application.appComponent(): AppComponent {
    return (this as TsuApplication).appComponent
}

sealed class ApiAccess {
    object None: ApiAccess()
    data class Token(val token: String): ApiAccess()

}
