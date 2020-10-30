package social.tsu.android.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import social.tsu.android.ui.user_profile.UserAboutFragment
import social.tsu.android.ui.user_profile.friends.UserFriendsFragment


@Module(includes = [UserProfileModule.BindsInstance::class])
class UserProfileModule {

    @Module
    interface BindsInstance {

        @ContributesAndroidInjector
        fun userFriendsFragment(): UserFriendsFragment

        @ContributesAndroidInjector
        fun userAboutFragment(): UserAboutFragment

    }

}