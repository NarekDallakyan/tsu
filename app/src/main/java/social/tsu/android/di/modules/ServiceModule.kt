package social.tsu.android.di.modules

import dagger.Module
import dagger.Provides
import social.tsu.android.TsuApplication
import social.tsu.android.service.*
import javax.inject.Singleton


@Module
class ServiceModule {

    @Provides
    @Singleton
    fun provideUserProfileImageService(application: TsuApplication): UserProfileImageService {
        return DefaultUserProfileImageService(application)
    }

    @Provides
    @Singleton
    fun provideCommunityMembersService(
        application: TsuApplication
    ): CommunityMembersService {
        return DefaultCommunityMembersService(application)
    }

    @Provides
    @Singleton
    fun provideUserAnalyticsService(application: TsuApplication): UserAnalyticsService {
        return DefaultUserAnalyticsService(application)
    }

    @Provides
    @Singleton
    fun provideUserFamilyTreeService(application: TsuApplication): UserFamilyTreeService {
        return DefaultUserFamilyTreeService(application)
    }

}