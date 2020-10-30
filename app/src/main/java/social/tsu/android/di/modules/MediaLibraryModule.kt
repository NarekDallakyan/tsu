package social.tsu.android.di.modules

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import social.tsu.android.data.repository.MediaLibraryRepository
import social.tsu.android.ui.new_post.library.MediaLibraryFragment

@Module(includes = [MediaLibraryModule.BindsInstance::class])
class MediaLibraryModule {

    @Provides
    fun providesMediaLibraryRepository(application: Application): MediaLibraryRepository {
        return MediaLibraryRepository(
            application
        )
    }


    @Module
    interface BindsInstance{

        @ContributesAndroidInjector
        fun mediaLibraryFragment(): MediaLibraryFragment

    }
}