package social.tsu.android.di.modules



import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import social.tsu.android.RxSchedulers
import social.tsu.android.data.repository.UserInviteRepository
import social.tsu.android.network.api.CreateAccountApi
import social.tsu.android.service.DefaultUserInviteService
import social.tsu.android.service.UserInviteService
import social.tsu.android.ui.user_invite.UserInviteEnterEmailFragment

@Module(includes = [UserInviteModule.BindsInstance::class])
class UserInviteModule {

    @Provides
    fun providesOldTsuUserervice(
        application: Application,
        createAccountApi: CreateAccountApi,
        schedulers: RxSchedulers
    ): UserInviteService {
        return DefaultUserInviteService(application, createAccountApi, schedulers)
    }

    @Provides
    fun providesOldTsuUserRepository(userInviteService: UserInviteService): UserInviteRepository {
        return UserInviteRepository(
            userInviteService
        )
    }


    @Module
    interface BindsInstance {


        @ContributesAndroidInjector
        fun userInviteEnterEmailFragment(): UserInviteEnterEmailFragment

    }
}