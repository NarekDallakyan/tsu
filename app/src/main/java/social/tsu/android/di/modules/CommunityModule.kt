package social.tsu.android.di.modules


import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import social.tsu.android.RxSchedulers
import social.tsu.android.data.repository.CommunityRepository
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.api.MembershipApi
import social.tsu.android.service.CommunitiesService
import social.tsu.android.service.DefaultCommunitiesService
import social.tsu.android.ui.community.CommunityFragment

@Module(includes = [CommunityModule.BindsInstance::class])
class CommunityModule {

    @Provides
    fun providesOldTsuUserervice(
        application: Application,
        communityApi: CommunityApi,
        membershipApi: MembershipApi,
        schedulers: RxSchedulers
    ): CommunitiesService {
        return DefaultCommunitiesService(application, communityApi, membershipApi, schedulers)
    }

    @Provides
    fun providesCommunityRepository(communitiesService: CommunitiesService): CommunityRepository {
        return CommunityRepository(
            communitiesService
        )
    }


    @Module
    interface BindsInstance {

        @ContributesAndroidInjector
        fun communityFragment(): CommunityFragment

    }
}