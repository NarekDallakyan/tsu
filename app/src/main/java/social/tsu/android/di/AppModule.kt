package social.tsu.android.di

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.paypal.android.sdk.payments.PayPalConfiguration
import com.paypal.android.sdk.payments.PayPalOAuthScopes
import dagger.Module
import dagger.Provides
import social.tsu.android.*
import social.tsu.android.data.local.LocalDatabase
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.rx.DefaultSchedulers
import social.tsu.android.service.PayPalConfigService
import social.tsu.android.service.SharedPrefManager
import java.util.*
import javax.inject.Singleton
import kotlin.collections.HashSet

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideSchedulers(): RxSchedulers = DefaultSchedulers()

    @Provides
    @Singleton
    fun provideFlipper(): Flipper = FlipperConfig()

    @Provides
    @Singleton
    fun provideApplication(context: Context): Application = context as Application

    @Provides
    @Singleton
    fun provideTsuApplication(application: Application): TsuApplication = application as TsuApplication

    @Provides
    @Singleton
    fun provideResources(context: Context): Resources = context.resources

    @Provides
    @Singleton
    fun provideNotificationManager(application: Application): NotificationManager {
        return application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    fun provideNotificationBuilder(application: Application): NotificationCompat.Builder {
        return NotificationCompat.Builder(application)
    }

    @Provides
    @Singleton
    fun provideGlideRequestManager(application: Application): RequestManager {
        return Glide.with(application)
    }

    @Provides
    @Singleton
    fun provideLocalDatabase(application: Application): LocalDatabase {
        return LocalDatabase.newInstance(application)

    }

    @Provides
    @Singleton
    fun providePayPalConfiguration(resources: Resources): PayPalConfiguration {
        val clientId = when {
            BuildConfig.DEBUG -> resources.getString(R.string.paypal_client_id_sandbox)
            else -> resources.getString(R.string.paypal_client_id_prod)
        }

        val environment = when {
            BuildConfig.DEBUG -> PayPalConfiguration.ENVIRONMENT_SANDBOX
            else -> PayPalConfiguration.ENVIRONMENT_PRODUCTION
        }

        return PayPalConfigService.createConfiguration(
            environment,
            clientId,
            resources.getString(R.string.app_name),
            Uri.parse(resources.getString(R.string.tsu_privacy_policy_url)),
            Uri.parse(resources.getString(R.string.tsu_terms_url))
        )
    }

    @Provides
    @Singleton
    fun providePayPalOauthScopes(): PayPalOAuthScopes {
        val scopes = HashSet(
            Arrays.asList(
                PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL,
                PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS
            )
        )
        return PayPalOAuthScopes(scopes)
    }

    @Provides
    @Singleton
    fun provideSharedPrefManager(application: Application): SharedPrefManager {
        return SharedPrefManager(application.getSharedPreferences("shared", Context.MODE_PRIVATE))
    }

    @Provides
    @Singleton
    fun provideAnalyticsHelper(application: Application, sharedPrefManager: SharedPrefManager) : AnalyticsHelper {
        return AnalyticsHelper(application,sharedPrefManager)
    }
}
