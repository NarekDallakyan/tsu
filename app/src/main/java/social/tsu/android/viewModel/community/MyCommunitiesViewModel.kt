package social.tsu.android.viewModel.community

import social.tsu.android.TsuApplication
import social.tsu.android.network.model.Membership
import social.tsu.android.service.DefaultMembershipService
import social.tsu.android.service.MembershipService
import social.tsu.android.service.MembershipServiceCallback


abstract class MyCommunitiesViewModel{
    var nextPage: Int? = null
    abstract fun getMyMemberships()
    abstract fun acceptPromotion(promotionId: Int)
    abstract fun declinePromotion(promotionId: Int)
    abstract fun acceptMembership(membershipId: Int)
    abstract fun declineMembership(membershipId: Int)
    abstract fun destroy()
}

interface MyCommunitiesViewModelCallback {
    fun didGetMyMemberships(memberships: List<Membership>)
    fun didWithError(message: String)

    fun didAcceptMembership(membershipId: Int)
    fun didDeclineMembership(membershipId: Int)
    fun didAcceptPromotion(membershipId: Int)
    fun didDeclinePromotion(membershipId: Int)
    fun errorAcceptMembership(membershipId: Int)
    fun errorDeclineMembership(membershipId: Int)
    fun errorAcceptPromotion(membershipId: Int)
    fun errorDeclinePromotion(membershipId: Int)
}

class DefaultMyCommunitiesViewModel(
    application: TsuApplication,
    private var callback: MyCommunitiesViewModelCallback?
) : MyCommunitiesViewModel(), MembershipServiceCallback {

    private val membershipService: MembershipService by lazy {
        DefaultMembershipService(application, this)
    }

    override fun getMyMemberships() {
        membershipService.getMemberships()
    }

    override fun acceptPromotion(promotionId: Int) {
        membershipService.acceptPromotion(promotionId)
    }

    override fun declinePromotion(promotionId: Int) {
        membershipService.declinePromotion(promotionId)
    }

    override fun acceptMembership(membershipId: Int) {
        membershipService.acceptMembership(membershipId)
    }

    override fun declineMembership(membershipId: Int) {
        membershipService.declineMembership(membershipId)
    }

    override fun destroy() {
        membershipService.onDestroy()
    }

    override fun completeGetMembership(memberships: List<Membership>) {
        callback?.didGetMyMemberships(memberships)
    }

    override fun completeAcceptMembership(membershipId: Int) {
        callback?.didAcceptMembership(membershipId)
    }

    override fun completeDeclineMembership(membershipId: Int) {
        callback?.didDeclineMembership(membershipId)
    }

    override fun completeAcceptPromotion(membershipId: Int) {
        callback?.didAcceptPromotion(membershipId)
    }

    override fun completeDeclinePromotion(membershipId: Int) {
        callback?.didDeclinePromotion(membershipId)
    }

    override fun completeDeleteMembership(membershipId: Int) {
    }

    override fun errorGetMembership(reason: String) {
        callback?.didWithError(reason)
    }

    override fun errorAcceptMembership(membershipId: Int) {
        callback?.errorAcceptMembership(membershipId)
    }

    override fun errorDeclineMembership(membershipId: Int) {
        callback?.errorDeclineMembership(membershipId)
    }

    override fun errorAcceptPromotion(membershipId: Int) {
        callback?.errorAcceptPromotion(membershipId)
    }

    override fun errorDeclinePromotion(membershipId: Int) {
        callback?.errorDeclinePromotion(membershipId)
    }

    override fun errorDeleteMembership(membershipId: Int) {
    }

    override fun didErrorWith(message: String) {
        callback?.didWithError(message)
    }

}