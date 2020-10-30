package social.tsu.android.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import social.tsu.android.ui.AdsSupportViewModel
import social.tsu.android.ui.BankAccountViewModel
import social.tsu.android.ui.InviteContactsViewModel
import social.tsu.android.ui.InviteFriendsViewModel
import social.tsu.android.ui.MainViewModel
import social.tsu.android.ui.hashtag.feed.HashtagFeedViewModel
import social.tsu.android.ui.live_stream.LiveStreamViewModel
import social.tsu.android.ui.messaging.chats.ChatViewModel
import social.tsu.android.ui.messaging.recents.RecentContactViewModel
import social.tsu.android.ui.messaging.tsu_contacts.followers.FollowersViewModel
import social.tsu.android.ui.messaging.tsu_contacts.followings.FollowingsViewModel
import social.tsu.android.ui.messaging.tsu_contacts.friends.FriendsViewModel
import social.tsu.android.ui.new_post.library.MediaLibraryViewModel
import social.tsu.android.ui.new_post.likes.LikesListViewModel
import social.tsu.android.ui.notifications.feed.NotificationsViewModel
import social.tsu.android.ui.notifications.subscriptions.NotificationSubscriptionViewModel
import social.tsu.android.ui.post_feed.comment.CommentViewModel
import social.tsu.android.ui.post_feed.community.CommunityFeedViewModel
import social.tsu.android.ui.post_feed.edit_post.EditPostViewModel
import social.tsu.android.ui.post_feed.main.MainFeedViewModel
import social.tsu.android.ui.post_feed.single_post.SinglePostFeedViewModel
import social.tsu.android.ui.post_feed.user_feed.UserFeedViewModel
import social.tsu.android.ui.reset_password.ResetPasswordViewModel
import social.tsu.android.ui.user_invite.UserInviteViewModel
import social.tsu.android.ui.user_profile.UserProfileViewModel
import social.tsu.android.ui.user_profile.insights.analytics.UserAnalyticsViewModel
import social.tsu.android.viewModel.signup.SignupViewModel

@Module
abstract class ViewModelModule {

    @Binds
    internal abstract fun bindsDaggerViewModelFactory(
        factory: DaggerViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(BankAccountViewModel::class)
    internal abstract fun accountViewModel(viewModel: BankAccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(InviteFriendsViewModel::class)
    internal abstract fun inviteFriendsViewModel(viewModel: InviteFriendsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(InviteContactsViewModel::class)
    internal abstract fun inviteContactsViewModel(viewModel: InviteContactsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ResetPasswordViewModel::class)
    internal abstract fun resetPasswordViewModel(viewModel: ResetPasswordViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    internal abstract fun mainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AdsSupportViewModel::class)
    internal abstract fun adsSupportViewModel(viewModel: AdsSupportViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SignupViewModel::class)
    internal abstract fun signupViewModel(viewModel: SignupViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(MediaLibraryViewModel::class)
    internal abstract fun mediaLibraryViewModel(viewModel: MediaLibraryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationsViewModel::class)
    internal abstract fun notificationsViewModel(viewModel: NotificationsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserProfileViewModel::class)
    internal abstract fun userProfileViewModel(viewModel: UserProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserAnalyticsViewModel::class)
    internal abstract fun userAnalyticsViewModelTest(viewModel: UserAnalyticsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FollowersViewModel::class)
    internal abstract fun followersViewModel(viewModel: FollowersViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(FollowingsViewModel::class)
    internal abstract fun followingsViewModel(viewModel: FollowingsViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(FriendsViewModel::class)
    internal abstract fun friendsViewModel(viewModel: FriendsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RecentContactViewModel::class)
    internal abstract fun recentContactViewModel(viewModel: RecentContactViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    internal abstract fun chatViewModel(viewModel: ChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationSubscriptionViewModel::class)
    internal abstract fun notificationSubscriptionViewModel(viewModel: NotificationSubscriptionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LikesListViewModel::class)
    internal abstract fun likesListViewModel(viewModel: LikesListViewModel): LikesListViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SinglePostFeedViewModel::class)
    internal abstract fun singlePostFeedViewModel(viewModel: SinglePostFeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CommentViewModel::class)
    internal abstract fun commentViewModel(viewModel: CommentViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserInviteViewModel::class)
    internal abstract fun oldTsuUserViewModel(inviteViewModel: UserInviteViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainFeedViewModel::class)
    internal abstract fun mainFeedViewModel(inviteViewModel: MainFeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserFeedViewModel::class)
    internal abstract fun userFeedViewModel(inviteViewModel: UserFeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HashtagFeedViewModel::class)
    internal abstract fun userHashtagFeedViewModel(inviteViewModel: HashtagFeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EditPostViewModel::class)
    internal abstract fun editPostViewModel(inviteViewModel: EditPostViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CommunityFeedViewModel::class)
    internal abstract fun communityFeedViewModel(inviteViewModel: CommunityFeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LiveStreamViewModel::class)
    internal abstract fun liveStreamViewModel(liveStreamViewModel: LiveStreamViewModel): ViewModel

}
