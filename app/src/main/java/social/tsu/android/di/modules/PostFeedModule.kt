package social.tsu.android.di.modules


import android.app.Application
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.LocalDatabase
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.repository.PostCommentRepository
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.network.api.*
import social.tsu.android.service.CommentActionsService
import social.tsu.android.service.DefaultCommentActionsService
import social.tsu.android.service.post_feed.*
import social.tsu.android.ui.hashtag.feed.HashtagFeedFragment
import social.tsu.android.ui.post_feed.comment.CommentsFragment
import social.tsu.android.ui.post_feed.community.CommunityFeedFragment
import social.tsu.android.ui.post_feed.edit_post.EditPostFragment
import social.tsu.android.ui.post_feed.main.MainFeedFragment
import social.tsu.android.ui.post_feed.single_post.SinglePostFeedFragment
import social.tsu.android.ui.post_feed.user_feed.UserFeedFragment
import social.tsu.android.ui.post_feed.user_feed.UserPhotosFeedFragment
import social.tsu.android.ui.post_feed.user_feed.UserVideosFeedFragment
import social.tsu.android.ui.search.DiscoveryFeedFragment
import social.tsu.android.ui.user_profile.UserSettingsFragment
import javax.inject.Singleton

@Module(includes = [PostFeedModule.BindsInstance::class])
class PostFeedModule {

    @Provides
    fun providesPostfeedService(
        application: Application,
        postApi: PostApi,
        communityApi: CommunityApi,
        schedulers: RxSchedulers
    ): PostFeedService {
        return DefaultPostFeedService(
            application,
            postApi,
            communityApi,
            schedulers
        )
    }

    @Provides
    fun providesPostFeedDao(localDatabase: LocalDatabase): PostFeedDao {
        return localDatabase.postFeedDao()
    }

    @Provides
    fun providesPostLikeService(
        application: Application,
        likeApi: LikeApi,
        schedulers: RxSchedulers
    ): PostLikeService {
        return DefaultPostLikeService(
            application,
            likeApi,
            schedulers
        )
    }

    @Provides
    fun providesPostShareService(
        application: Application,
        shareAPI: ShareAPI,
        schedulers: RxSchedulers
    ): PostShareService {
        return DefaultPostShareService(
            application,
            shareAPI,
            schedulers
        )
    }

    @Provides
    fun providesPostSupportService(
        application: Application,
        supportApi: SupportApi,
        schedulers: RxSchedulers
    ): PostSupportService {
        return DefaultPostSupportService(
            application,
            supportApi,
            schedulers
        )
    }

    @Provides
    fun providesPostBlockService(
        application: Application,
        blockApi: BlockApi,
        schedulers: RxSchedulers
    ): PostBlockService {
        return DefaultPostBlockService(
            application,
            blockApi,
            schedulers
        )
    }

    @Provides
    fun providesCommentActionsService(
        application: Application,
        commentApi: CommentAPI,
        schedulers: RxSchedulers
    ): CommentActionsService {
        return DefaultCommentActionsService(
            application,
            commentApi,
            schedulers
        )
    }

    @Singleton
    @Provides
    fun providesPostFeedRepository(
        postFeedService: PostFeedService,
        postLikeService: PostLikeService,
        postBlockService: PostBlockService,
        postShareService: PostShareService,
        postSupportService: PostSupportService,
        postFeedDao: PostFeedDao
    ): PostFeedRepository {
        return PostFeedRepository(
            postFeedService,
            postLikeService,
            postShareService,
            postBlockService,
            postSupportService,
            postFeedDao
        )
    }

    @Singleton
    @Provides
    fun providesPostCommentRepository(
        commentActionsService: CommentActionsService,
        postFeedDao: PostFeedDao
    ): PostCommentRepository {
        return PostCommentRepository(commentActionsService, postFeedDao)
    }

    @Module
    interface BindsInstance {

        @ContributesAndroidInjector
        fun singlePostFeedFragment(): SinglePostFeedFragment

        @ContributesAndroidInjector
        fun commentFragment(): CommentsFragment

        @ContributesAndroidInjector
        fun mainFeedFragment(): MainFeedFragment

        @ContributesAndroidInjector
        fun communityFeedFragment(): CommunityFeedFragment

        @ContributesAndroidInjector
        fun userPhotosFeedFragment(): UserPhotosFeedFragment

        @ContributesAndroidInjector
        fun hashtagFeedFragment(): HashtagFeedFragment

        @ContributesAndroidInjector
        fun userVideosFeedFragment(): UserVideosFeedFragment

        @ContributesAndroidInjector
        fun discoveryFeedFragment(): DiscoveryFeedFragment

        @ContributesAndroidInjector
        fun userFeedFragment(): UserFeedFragment

        @ContributesAndroidInjector
        fun editPostFragment(): EditPostFragment

        @ContributesAndroidInjector
        fun userSettingsFragment(): UserSettingsFragment

    }

}