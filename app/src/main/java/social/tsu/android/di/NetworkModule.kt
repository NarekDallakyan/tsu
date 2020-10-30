package social.tsu.android.di

import android.app.Application
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import social.tsu.android.Flipper
import social.tsu.android.RxSchedulers
import social.tsu.android.SearchApi
import social.tsu.android.TsuApplication
import social.tsu.android.network.ApiGatewayInterceptor
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.TokenInterceptor
import social.tsu.android.network.api.*
import social.tsu.android.service.AuthenticationService
import social.tsu.android.service.DefaultFriendService
import social.tsu.android.service.FriendService
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

@Module
@SuppressWarnings("TooManyFunctions")
class NetworkModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {

        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
//            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    @Named("default")
    fun provideTokenInterceptor(application:Application,
                                authenticationApi: AuthenticationApi,
                                @Named("encrypted") sharedPrefs: SharedPreferences): TokenInterceptor {
        return TokenInterceptor(application,authenticationApi,sharedPrefs, Environment.key)
    }

    @Provides
    @Singleton
    @Named("notification")
    fun provideNotificationTokenInterceptor(application:Application,
                                            authenticationApi: AuthenticationApi,
                                            @Named("encrypted") sharedPrefs: SharedPreferences): TokenInterceptor {
        return TokenInterceptor(application,authenticationApi,sharedPrefs, Environment.notificationsKey)
    }

    @Provides
    @Singleton
    fun provideApiGatewayInterceptor(): ApiGatewayInterceptor {
        return ApiGatewayInterceptor()
    }

    @Provides
    @Singleton
    @Named(NAMED_OKHTTP_CLIENT_DEFAULT)
    fun provideDefaultOkHttpClient(
        apiGatewayInterceptor: ApiGatewayInterceptor,
        flipper: Flipper
    ): OkHttpClient {

        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }

        val builder = OkHttpClient.Builder()
        builder.addInterceptor(apiGatewayInterceptor)
        builder.addInterceptor(logging)
        // TODO: Determine way to exclude certain routes from flipper interceptor
        flipper.addInterceptors(builder)

        return builder.
        readTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            .connectTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            .build()
    }

    @Provides
    @Singleton
    @Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED)
    fun provideAuthenticatedOkHttpClient(
        @Named("default") tokenInterceptor: TokenInterceptor,
        flipper: Flipper
    ): OkHttpClient {

        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }

        val builder = OkHttpClient.Builder()
        builder.addInterceptor(tokenInterceptor)
        builder.addInterceptor(logging)
        // TODO: Determine way to exclude certain routes from flipper interceptor
        flipper.addInterceptors(builder)

        return builder.
        readTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            .connectTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            .build()
    }

    @Provides
    @Singleton
    @Named(NAMED_OKHTTP_CLIENT_NOTIFICATION)
    fun provideNotificationOkHttpClient(
        @Named("notification") tokenInterceptor: TokenInterceptor,
        flipper: Flipper
    ): OkHttpClient {

        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }

        val builder = OkHttpClient.Builder()
        builder.addInterceptor(tokenInterceptor)
        builder.addInterceptor(logging)
        // TODO: Determine way to exclude certain routes from flipper interceptor
        flipper.addInterceptors(builder)

        return builder.
        readTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            .connectTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            .build()
    }

    @Provides
    @Singleton
    @Named(NAMED_OKHTTP_CLIENT_AUTH)
    fun provideAuthOkHttpClient(apiGatewayInterceptor: ApiGatewayInterceptor): OkHttpClient {

        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
        val dispatcher = Dispatcher().apply { maxRequests = 1 }

        return OkHttpClient.Builder().apply {
            addInterceptor(logging)
            addInterceptor(apiGatewayInterceptor)
            dispatcher(dispatcher)
            readTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            connectTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
        }.build()
    }

    @Provides
    @Singleton
    fun provideAuthenticationApi(
        moshi: Moshi,
        @Named(NAMED_OKHTTP_CLIENT_AUTH) authOkHttpClient: OkHttpClient
    ): AuthenticationApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(authOkHttpClient)
            .build()

        return retrofit.create(AuthenticationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthenticationService(
        application: TsuApplication, authenticationApi: AuthenticationApi,
        userSettingsApi: UserSettingsApi,
        schedulers: RxSchedulers
    ): AuthenticationService {
        return AuthenticationService(application, authenticationApi, userSettingsApi, schedulers)
    }


    @Provides
    fun provideFriendService(
        application: Application, friendsApi: FriendsApi,
        schedulers: RxSchedulers
    ): FriendService {
        return DefaultFriendService(application, friendsApi, schedulers)
    }

    @Provides
    @Singleton
    fun providePostApi(
        @Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient,
        moshi: Moshi
    ): PostApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

        return retrofit.create(PostApi::class.java)
    }

    @Provides
    @Singleton
    fun providePostImageApi(
        @Named("default") tokenInterceptor: TokenInterceptor,
        moshi: Moshi
    ): PostImageApi {
        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }
        val dispatcher = Dispatcher().apply { maxRequests = 1 }

        val client = OkHttpClient.Builder().apply {
            addInterceptor(logging)
            addInterceptor(tokenInterceptor)
            dispatcher(dispatcher)
            writeTimeout(NetworkConstants.IMAGE_UPLOAD_TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            readTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
            connectTimeout(NetworkConstants.TIMEOUT, NetworkConstants.TIMEOUT_UNIT)
        }.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(client)
            .build()

        return retrofit.create(PostImageApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(
        @Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient,
        moshi: Moshi
    ): UserApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserSettingsApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): UserSettingsApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

        return retrofit.create(UserSettingsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): SearchApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

        return retrofit.create(SearchApi::class.java)
    }


    @Provides
    @Singleton
    fun provideCreateAccountAPI(@Named(NAMED_OKHTTP_CLIENT_DEFAULT) okHttpClient: OkHttpClient, moshi: Moshi): CreateAccountApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(CreateAccountApi::class.java)
    }
    @Provides
    @Singleton
    fun provideFriendsApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): FriendsApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(FriendsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLikeApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): LikeApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(LikeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSupportApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): SupportApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(SupportApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCommentApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): CommentAPI {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(CommentAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideCommunityApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): CommunityApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(CommunityApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFeedApi(@Named(NAMED_OKHTTP_CLIENT_DEFAULT) okHttpClient: OkHttpClient, moshi: Moshi): FeedApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(FeedApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLiveApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): LiveApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(LiveApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): StreamApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(StreamApi::class.java)
    }

    @Provides
    @Singleton
    fun provideShareApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): ShareAPI {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(ShareAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideBlockApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): BlockApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(BlockApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAccount(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): AccountApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(AccountApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReferralApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): ReferralApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(ReferralApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMembershipApi(@Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient, moshi: Moshi): MembershipApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(MembershipApi::class.java)
    }

    @Provides
    @Singleton
    fun provideConfigurationApi(
        @Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ConfigurationApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(ConfigurationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAnalyticsApi(
        @Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient,
        moshi: Moshi
    ): AnalyticsApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(AnalyticsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDiscoveryApi(
        @Named(NAMED_OKHTTP_CLIENT_AUTHENTICATED) okHttpClient: OkHttpClient,
        moshi: Moshi
    ): DiscoveryApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(DiscoveryApi::class.java)
    }

    companion object {
        const val NAMED_OKHTTP_CLIENT_AUTH = "auth"
        const val NAMED_OKHTTP_CLIENT_DEFAULT = "default"
        const val NAMED_OKHTTP_CLIENT_AUTHENTICATED = "authenticated"
        const val NAMED_OKHTTP_CLIENT_NOTIFICATION = "notification"
    }
}