package social.tsu.android.ui.post_feed.community


import social.tsu.android.data.repository.CommunityRepository
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.PendingPost
import social.tsu.android.ui.post_feed.BaseFeedViewModel
import javax.inject.Inject

class CommunityFeedViewModel @Inject constructor(
    private val communityRepo: CommunityRepository,
    private val postFeedRepo: PostFeedRepository
) : BaseFeedViewModel(postFeedRepo) {

    var pendingPosts: List<PendingPost>? = null

    val userRefreshLoadState by lazy {
        postFeedRepo.userRefreshLoadState
    }

    fun getCommunityPosts(communityId: Int) = postFeedRepo.getCommunityPosts(communityId)

    fun getPendingPosts(communityId: Int) = communityRepo.getPendingPosts(communityId)

    fun leave(membershipId: Int) = communityRepo.leave(membershipId)

    fun joinCommunity(community: Group) = communityRepo.joinCommunity(community)

    fun acceptMembership(membershipId: Int) = communityRepo.acceptMembership(membershipId)

    fun loadCommunity(communityId: Int) = communityRepo.loadCommunity(communityId)

    fun loadMembership(communityId: Int) = communityRepo.loadMembership(communityId)

    fun refreshCommunityPosts(communityId: Int, userInitiatedRefresh: Boolean = false) =
        postFeedRepo.refreshCommunityPosts(communityId, userInitiatedRefresh)

    fun retry() {
        postFeedRepo.retry()
    }

}

