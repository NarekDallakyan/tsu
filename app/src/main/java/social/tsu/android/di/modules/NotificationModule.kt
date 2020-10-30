package social.tsu.android.di.modules

import android.app.Application
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.LocalDatabase
import social.tsu.android.data.local.dao.TsuNotificationDao
import social.tsu.android.data.repository.TsuNotificationRepository
import social.tsu.android.di.NetworkModule
import social.tsu.android.network.api.HostEndpoint
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.api.NotificationsApi
import social.tsu.android.service.DefaultTsuNotificationsService
import social.tsu.android.service.FriendService
import social.tsu.android.service.TsuNotificationsService
import social.tsu.android.ui.notifications.feed.NotificationFragment
import social.tsu.android.ui.notifications.subscriptions.NotificationSubscriptionsFragment
import javax.inject.Named

@Module(includes = [NotificationModule.BindsInstance::class])
class NotificationModule {

    @Provides
    fun provideNotificationsApi(@Named(NetworkModule.NAMED_OKHTTP_CLIENT_NOTIFICATION) okHttpClient: OkHttpClient): NotificationsApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.notification))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .client(okHttpClient)
            .build()

        return retrofit.create(NotificationsApi::class.java)
    }

    @Provides
    fun providesTsuNotificationsService(
        application: Application,
        notificationsApi: NotificationsApi,
        schedulers: RxSchedulers
    ): TsuNotificationsService {
        return DefaultTsuNotificationsService(application, notificationsApi, schedulers)
    }

    @Provides
    fun providesTsuNotificationDao(localDatabase: LocalDatabase): TsuNotificationDao {
        return localDatabase.tsuNotificationDao()
    }

    @Provides
    fun providesTsuNotificationRepository(tsuNotificationsService: TsuNotificationsService,
                                          friendService: FriendService,
                                        tsuNotificationDao: TsuNotificationDao): TsuNotificationRepository {
        return TsuNotificationRepository(tsuNotificationsService, friendService, tsuNotificationDao)
    }


    @Module
    interface BindsInstance{

        @ContributesAndroidInjector
        fun notificationFragment(): NotificationFragment

        @ContributesAndroidInjector
        fun notificationSubscriptionsFragment(): NotificationSubscriptionsFragment

    }
}