package social.tsu.android.viewModel.community

import social.tsu.android.TsuApplication
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.PendingPost
import social.tsu.android.network.model.PendingRequest
import social.tsu.android.service.CommunityService
import social.tsu.android.service.CommunityServiceCallback
import social.tsu.android.service.DefaultCommunityService


abstract class CommunitySuggestedViewModel {
    abstract fun loadSuggestedCommunities()
    abstract fun joinCommunity(group: Group)
    abstract fun onDestroy()
}

interface CommunitySuggestedViewModelCallback {
    fun didLoadCommunitySuggested(communities: List<Group>)
    fun didFailCommunitySuggested(message: String)
    fun didJoinCommunity(group: Group)
    fun failedToJoin(group: Group)
    fun didErrorWith(message: String)
}

class DefaultCommunitySuggestedViewModel(
    application: TsuApplication,
    private var callback: CommunitySuggestedViewModelCallback?
) : CommunitySuggestedViewModel(), CommunityServiceCallback {

    private val communityService: CommunityService by lazy {
        DefaultCommunityService(application, this)
    }

    override fun onDestroy() {
        communityService.onDestroy()
    }

    override fun didErrorWith(message: String) {
        callback?.didErrorWith(message)
    }

    override fun loadSuggestedCommunities() {
        communityService.loadSuggestedCommunities()
    }

    override fun didCreateCommunity() {
        // ignore
    }

    override fun joinCommunity(group: Group) {
        communityService.joinCommunity(group)
    }

    override fun didLoadCommunity(community: Group) {
    }

    override fun didLoadSuggestedCommunities(communities: List<Group>) {
        callback?.didLoadCommunitySuggested(communities)
    }

    override fun didLoadCommunityPendingRequests(requests: List<PendingRequest>) {
    }

    override fun didJoinCommunity(community: Group) {
        callback?.didJoinCommunity(community)
    }

    override fun didFailedToJoinCommunity(community: Group) {
        callback?.failedToJoin(community)
    }

    override fun completedGetCommunityPosts(nextPage: Int?) {
    }

    override fun failedGetCommunityPosts(code: Int) {
    }

    override fun failedLoadCommunityPendingRequests() {

    }

    override fun didLoadPendingPosts(posts: List<PendingPost>) {

    }

    override fun didFailToLoadPendingPosts(message: String) {
    }

    override fun didLeftGroup() {
    }

    override fun didFailedToLeaveGroup(message: String) {}

    override fun didDeleteCommunity() {
    }

    override fun didUpdateCommunity(group: Group) {
    }
}