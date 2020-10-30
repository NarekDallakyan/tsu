package social.tsu.android.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import social.tsu.android.ui.MainActivity

@Module
abstract class ActivityBuilder {

    @ContributesAndroidInjector
    abstract fun mainActivity(): MainActivity

}