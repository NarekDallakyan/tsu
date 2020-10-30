package social.tsu.android.di.modules


import android.app.Application
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.LocalDatabase
import social.tsu.android.data.repository.LogoutRepository
import social.tsu.android.di.NetworkModule
import social.tsu.android.network.api.HostEndpoint
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.api.LogoutApi
import social.tsu.android.service.DefaultLogoutService
import social.tsu.android.service.LogoutService
import javax.inject.Named
import javax.inject.Singleton

@Module
class LogoutModule {

    @Provides
    fun provideLogoutApi(@Named(NetworkModule.NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): LogoutApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(LogoutApi::class.java)
    }

    @Provides
    fun providesLogoutService(
        application: Application,
        logoutApi: LogoutApi,
        schedulers: RxSchedulers
    ): LogoutService {
        return DefaultLogoutService(
            application,
            logoutApi,
            schedulers
        )
    }

    @Provides
    fun providesLogoutRepository(
        application: Application,
        logoutService: LogoutService,
        @Named("encrypted") sharedPrefs: SharedPreferences,
        localDatabase: LocalDatabase
    ): LogoutRepository {
        return LogoutRepository(application, logoutService, sharedPrefs, localDatabase)
    }

    @Provides
    @Singleton
    @Named("encrypted")
    fun providesEncryptedSharePrefs(application: Application): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "PreferencesFilename",
            masterKeyAlias,
            application.applicationContext!!,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

}