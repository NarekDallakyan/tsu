package social.tsu.android.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import social.tsu.android.ui.new_post.likes.LikesListFragment
import social.tsu.android.ui.new_post.supports.SupportsListFragment


@Module
interface PostModule {

    @ContributesAndroidInjector
    fun likesListFragment(): LikesListFragment

    @ContributesAndroidInjector
    fun supportsListFragment(): SupportsListFragment

}