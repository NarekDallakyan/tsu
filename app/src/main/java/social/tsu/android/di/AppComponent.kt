package social.tsu.android.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import social.tsu.android.TsuApplication
import social.tsu.android.di.modules.*
import social.tsu.android.service.*
import social.tsu.android.service.post_feed.DefaultPostSupportService
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.StartupActivity
import social.tsu.android.viewModel.community.CommunityFeedViewModel
import social.tsu.android.viewModel.community.DefaultCommunityPublishingRequestsViewModel
import social.tsu.android.viewModel.community.PendingMembershipViewModel
import social.tsu.android.viewModel.editPost.DefaultEditPostViewModel
import social.tsu.android.viewModel.mainFeedViewModel.DefaultMainFeedViewModel
import social.tsu.android.viewModel.userProfile.*
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidSupportInjectionModule::class,
    ActivityBuilder::class,
    ResetPasswordModule::class,
    LogoutModule::class,
    MediaLibraryModule::class,
    NotificationModule::class,
    ServiceBuilder::class,
    TsuContactsModule::class,
    MessagingModule::class,
    ChatModule::class,
    ServiceModule::class,
    PostModule::class,
    PostFeedModule::class,
    UserProfileModule::class,
    UserInviteModule::class,
    CommunityModule::class,
    LiveStreamChatsModule::class,
    NetworkModule::class, AppModule::class, AppSubcomponents::class, ViewModelModule::class])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun workerSubcomponentBuilder(): WorkerSubcomponent.Builder

    fun fragmentComponent(): FragmentComponent.Factory

    fun viewHolderComponent(): ViewHolderComponent.Factory

    fun inject(application: TsuApplication)
    fun inject(mainActivity: MainActivity)
    fun inject(startupActivity: StartupActivity)
    fun inject(defaultMainFeedViewModel: DefaultMainFeedViewModel)
    fun inject(defaultUserInfoService: DefaultUserInfoService)
    fun inject(defaultSettingsService: DefaultSettingsService)
    fun inject(defaultSearchService: DefaultSearchService)
    fun inject(defaultFeedService: DefaultFeedService)
    fun inject(defaultCommunityService: DefaultCommunityService)
    fun inject(defaultCommunityFeedService: DefaultCommunityFeedService)
    fun inject(defaultUserPhotosService: DefaultUserPhotosService)
    fun inject(defaultUserVideosService: DefaultUserVideosService)
    fun inject(defaultHashtagGridService: DefaultHashtagGridService)
    fun inject(defaultDiscoveryGridService: DefaultDiscoveryGridService)
    fun inject(defaultMembershipService: DefaultMembershipService)
    fun inject(defaultCommunityMembersService: DefaultCommunityMembersService)
    fun inject(defaultUserAnalyticsService: DefaultUserAnalyticsService)
    fun inject(defaultUserFamilyTreeService: DefaultUserFamilyTreeService)

    fun inject(defaultEditPostViewModel: DefaultEditPostViewModel)
    fun inject(defaultLikeService: DefaultLikeService)
    fun inject(defaultCommentsService: DefaultCommentsService)
    fun inject(defaultShareService: DefaultShareService)
    fun inject(defaultSupportService: DefaultPostSupportService)
    fun inject(defaultUserFeedViewModel: DefaultUserFeedViewModel)
    fun inject(defaultUserPhotosViewModel: UserPhotosViewModel)
    fun inject(defaultBlockService: DefaultBlockService)
    fun inject(defaultUserVideosViewModel: UserVideosViewModel)
    fun inject(defaultUserPhotosFeedViewModel: CommunityFeedViewModel)
    fun inject(defaultCommunityPublishingRequestsViewModel: DefaultCommunityPublishingRequestsViewModel)
    fun inject(defaultPendingMembershipViewModel: PendingMembershipViewModel)
    fun inject(defaultReportService: DefaultReportService)
}
