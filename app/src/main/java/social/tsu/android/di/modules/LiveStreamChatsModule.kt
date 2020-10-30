package social.tsu.android.di.modules


import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import social.tsu.android.TsuApplication
import social.tsu.android.data.repository.LiveStreamChatsRepository
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.service.AuthenticationService
import social.tsu.android.ui.live_stream.LivestreamFragment

@Module(includes = [LiveStreamChatsModule.BindsInstance::class])
class LiveStreamChatsModule {

    @Provides
    fun providesLiveStreamChatsRepository(
        application: TsuApplication,
        authenticationService: AuthenticationService,
        analyticsHelper: AnalyticsHelper
    ): LiveStreamChatsRepository {
        return LiveStreamChatsRepository(
            application, authenticationService, analyticsHelper
        )
    }

    @Module
    interface BindsInstance {


        @ContributesAndroidInjector
        fun livestreamFragment(): LivestreamFragment

    }
}