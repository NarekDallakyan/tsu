package social.tsu.android.di

import dagger.Subcomponent
import social.tsu.android.di.annotation.FragmentScope
import social.tsu.android.ui.*
import social.tsu.android.ui.community.CommunityPublishingRequestsFragment
import social.tsu.android.ui.community.members.CommunityMembersFragment
import social.tsu.android.ui.live_stream.LivestreamFragment
import social.tsu.android.ui.new_post.PostDraftFragment
import social.tsu.android.ui.notifications.feed.NotificationFragment
import social.tsu.android.ui.post_feed.feed_type.FeedTypeFragment
import social.tsu.android.ui.post_feed.main.MainFeedFragment
import social.tsu.android.ui.search.DiscoveryFeedFragment
import social.tsu.android.ui.search.MentionUserSearchFragment
import social.tsu.android.ui.search.SearchFragment
import social.tsu.android.ui.user_profile.UserPhotosFragment
import social.tsu.android.ui.user_profile.UserProfileFragment
import social.tsu.android.ui.user_profile.UserVideosFragment
import social.tsu.android.ui.user_profile.insights.analytics.UserAnalyticsFragment
import social.tsu.android.ui.user_profile.insights.family_tree.UserFamilyTreeFragment
import social.tsu.android.ui.util.TutorialDialog


@FragmentScope
@Subcomponent(modules = [FragmentModule::class])
interface FragmentComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): FragmentComponent
    }

    fun inject(fragment: LoginFragment)
    fun inject(fragment: CommunityMembersFragment)
    fun inject(fragment: UserPhotosFragment)
    fun inject(fragment: UserVideosFragment)

    fun inject(fragment: SearchFragment)
    fun inject(fragment: MentionUserSearchFragment)
    fun inject(fragment: CreateAccountFragment)
    fun inject(userProfileFragment: UserProfileFragment)

    fun inject(postDraftFragment: PostDraftFragment)

    fun inject(notificationFragment: NotificationFragment)
    fun inject(bankAccountFragment: BankAccountFragment)
    fun inject(redeemFragment: RedeemFragment)
    fun inject(feedTypeFragment: FeedTypeFragment)

    fun inject(inviteFriendsFragment: InviteFriendsFragment)
    fun inject(inviteContactsFragment: InviteContactsFragment)

    fun inject(editAccountFragment: EditAccountFragment)
    fun inject(livestreamFragment: LivestreamFragment)
    fun inject(userFamilyTreeFragment: UserFamilyTreeFragment)
    fun inject(userAnalyticsFragment: UserAnalyticsFragment)
    fun inject(tutorialDialog: TutorialDialog)

    fun inject(mainFeedFragment: MainFeedFragment)
    fun inject(communityPublishingRequestsFragment: CommunityPublishingRequestsFragment)
    fun inject(discoveryFeedFragment: DiscoveryFeedFragment)
}

