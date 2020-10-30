package social.tsu.android.di.modules


import android.app.Application
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import social.tsu.android.RxSchedulers
import social.tsu.android.data.repository.ResetPasswordRepository
import social.tsu.android.di.NetworkModule
import social.tsu.android.network.api.HostEndpoint
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.api.ResetPasswordApi
import social.tsu.android.service.reset_password.DefaultResetPasswordService
import social.tsu.android.service.reset_password.ResetPasswordService
import social.tsu.android.ui.reset_password.ChangePasswordFragment
import social.tsu.android.ui.reset_password.EnterEmailFragment
import social.tsu.android.ui.reset_password.OneTimeCodeFragment
import javax.inject.Named

@Module(includes = [ResetPasswordModule.BindsInstance::class])
class ResetPasswordModule {

    @Provides
    fun provideResetPasswordApi(@Named(NetworkModule.NAMED_OKHTTP_CLIENT_DEFAULT) okHttpClient: OkHttpClient, moshi: Moshi): ResetPasswordApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(HostProvider.host(HostEndpoint.api))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(ResetPasswordApi::class.java)
    }

    @Provides
    fun providesResetPasswordService(
        application: Application,
        resetPasswordApi: ResetPasswordApi,
        schedulers: RxSchedulers
    ): ResetPasswordService {
        return DefaultResetPasswordService(application, resetPasswordApi, schedulers)
    }

    @Provides
    fun providesResetPasswordRepository(resetPasswordService: ResetPasswordService): ResetPasswordRepository {
        return ResetPasswordRepository(
            resetPasswordService
        )
    }


    @Module
    interface BindsInstance{


        @ContributesAndroidInjector
        fun enterEmailFragment(): EnterEmailFragment

        @ContributesAndroidInjector
        fun oneTimeCodeFragment(): OneTimeCodeFragment

        @ContributesAndroidInjector
        fun changePasswordFragment(): ChangePasswordFragment

    }
}