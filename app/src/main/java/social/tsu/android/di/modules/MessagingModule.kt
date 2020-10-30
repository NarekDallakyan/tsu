package social.tsu.android.di.modules

import android.app.Application
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.LocalDatabase
import social.tsu.android.data.local.dao.MessagingDao
import social.tsu.android.data.repository.MessagingRepository
import social.tsu.android.di.NetworkModule
import social.tsu.android.network.api.HostEndpoint
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.api.MessagingApi
import social.tsu.android.service.DefaultMessagingService
import social.tsu.android.service.MessagingService
import social.tsu.android.ui.messaging.recents.RecentContactsFragment
import javax.inject.Named
import javax.inject.Singleton


@Module(includes = [MessagingModule.BindsInstance::class])
class MessagingModule {

    @Provides
    @Singleton
    fun provideMessagingApi(@Named(NetworkModule.NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient): MessagingApi {
        val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

        return retrofit.create(MessagingApi::class.java)
    }

    @Provides
    fun providesMessagingService(
        application: Application,
        messagingApi: MessagingApi,
        rxSchedulers: RxSchedulers
    ): MessagingService {
        return DefaultMessagingService(application, messagingApi, rxSchedulers)
    }

    @Provides
    fun providesMessagingDao(localDatabase: LocalDatabase): MessagingDao {
        return localDatabase.messagingDao()
    }

    @Provides
    fun providesMessagingRepository(application: Application, messagingService: MessagingService, messagingDao: MessagingDao): MessagingRepository {
        return MessagingRepository(application, messagingService, messagingDao)
    }

    @Module
    interface BindsInstance{

        @ContributesAndroidInjector
        fun recentContactsFragment():RecentContactsFragment

    }

}