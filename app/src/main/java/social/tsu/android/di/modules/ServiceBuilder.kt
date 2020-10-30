package social.tsu.android.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import social.tsu.android.notifications.FCMService

@Module
abstract class ServiceBuilder {

    @ContributesAndroidInjector
    abstract fun fcmService(): FCMService

}