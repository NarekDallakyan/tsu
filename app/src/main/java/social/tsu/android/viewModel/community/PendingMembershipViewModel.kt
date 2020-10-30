package social.tsu.android.viewModel.community

import social.tsu.android.TsuApplication
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.Membership
import social.tsu.android.network.model.PendingPost
import social.tsu.android.network.model.PendingRequest
import social.tsu.android.service.*


abstract class PendingMembershipViewModel {
    var nextPage: Int? = null
    abstract fun getPendingMemberships(groupId: Int)
    abstract fun acceptPendingMembership(pending: PendingRequest)
    abstract fun denyPendingMembership(pending: PendingRequest)
}

interface PendingMembershipViewModelCallback {
    fun didGetPendingMemberships(memberships: List<PendingRequest>)
    fun didWithError(message: String)
    fun didAcceptMembership(pendingId: Int)
    fun didDeclineMembership(pendingId: Int)
}

class DefaultPendingMembershipViewModel(
    private val application: TsuApplication,
    private var callback: PendingMembershipViewModelCallback?
) : PendingMembershipViewModel(), MembershipServiceCallback,
    CommunityServiceCallback {

    private val membershipService: MembershipService by lazy {
        DefaultMembershipService(application, this)
    }

    private val communityService: CommunityService by lazy {
        DefaultCommunityService(application, this)
    }


    override fun getPendingMemberships(groupId: Int) {
        communityService.getCommunityPendingRequests(groupId)
    }

    override fun acceptPendingMembership(pending: PendingRequest) {
        membershipService.acceptMembership(pending.membershipId)
    }

    override fun denyPendingMembership(pending: PendingRequest) {
        membershipService.deleteMembership(pending.membershipId)
    }


    override fun didErrorWith(message: String) {
        callback?.didWithError(message)
    }

    override fun didLoadCommunity(community: Group) {
    }

    override fun completeAcceptMembership(membershipId: Int) {
        callback?.didAcceptMembership(membershipId)
    }

    override fun completeDeleteMembership(membershipId: Int) {
        callback?.didDeclineMembership(membershipId)
    }

    override fun didLoadCommunityPendingRequests(requests: List<PendingRequest>) {
        callback?.didGetPendingMemberships(requests)
    }

    override fun failedLoadCommunityPendingRequests() {
        callback?.didWithError("")
    }

    override fun didLoadPendingPosts(posts: List<PendingPost>) {
    }

    override fun didFailToLoadPendingPosts(message: String) {
    }

    override fun didLeftGroup() {
    }

    override fun didFailedToLeaveGroup(message: String) {
    }

    override fun didDeleteCommunity() {

    }

    override fun didUpdateCommunity(group: Group) {

    }

    override fun errorAcceptMembership(membershipId: Int) {
        callback?.didWithError("")
    }

    override fun errorDeleteMembership(membershipId: Int) {
        callback?.didWithError("")
    }


    //not needed

    override fun completeGetMembership(memberships: List<Membership>) {
    }

    override fun completeDeclineMembership(membershipId: Int) {
    }

    override fun completeAcceptPromotion(membershipId: Int) {
    }

    override fun completeDeclinePromotion(membershipId: Int) {
    }

    override fun errorGetMembership(reason: String) {
    }


    override fun errorDeclineMembership(membershipId: Int) {
    }

    override fun errorAcceptPromotion(membershipId: Int) {
    }

    override fun errorDeclinePromotion(membershipId: Int) {
    }



    override fun didCreateCommunity() {
    }

    override fun didLoadSuggestedCommunities(communities: List<Group>) {
    }



    override fun didJoinCommunity(community: Group) {
    }

    override fun didFailedToJoinCommunity(community: Group) {
    }

    override fun completedGetCommunityPosts(nextPage: Int?) {
    }

    override fun failedGetCommunityPosts(code: Int) {
    }






}