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
import social.tsu.android.TsuApplication
import social.tsu.android.data.repository.TsuContactsRepository
import social.tsu.android.di.NetworkModule
import social.tsu.android.network.api.HostEndpoint
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.api.TsuContactsApi
import social.tsu.android.service.DefaultTsuContactsService
import social.tsu.android.service.TsuContactsService
import social.tsu.android.ui.messaging.tsu_contacts.followers.FollowersListFragment
import social.tsu.android.ui.messaging.tsu_contacts.followings.FollowingsListFragment
import social.tsu.android.ui.messaging.tsu_contacts.friends.FriendsListFragment
import javax.inject.Named
import javax.inject.Singleton


@Module(includes = [TsuContactsModule.BindsInstance::class])
class TsuContactsModule {

    @Provides
    @Singleton
    fun provideTsuContactsApi(@Named(NetworkModule.NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient): TsuContactsApi {
        val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

        return retrofit.create(TsuContactsApi::class.java)
    }

    @Provides
    fun providesTsuContactsService(application: TsuApplication, tsuContactsApi: TsuContactsApi, rxSchedulers: RxSchedulers):TsuContactsService{
        return DefaultTsuContactsService(application,tsuContactsApi,rxSchedulers)
    }

    @Provides
    fun providesTsuContactsRepository(application:Application, tsuContactsService: TsuContactsService): TsuContactsRepository {
        return TsuContactsRepository(application, tsuContactsService)
    }

    @Module
    interface BindsInstance{

        @ContributesAndroidInjector
        fun friendsFragment():FriendsListFragment

        @ContributesAndroidInjector
        fun followingsFragment():FollowingsListFragment

        @ContributesAndroidInjector
        fun followersFragment():FollowersListFragment

    }

}