package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.MembershipApi
import social.tsu.android.network.model.Membership
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

abstract class MembershipService : DefaultService() {

    abstract fun getMemberships()
    abstract fun acceptPromotion(promotionId: Int)
    abstract fun declinePromotion(promotionId: Int)
    abstract fun acceptMembership(membershipId: Int)
    abstract fun declineMembership(membershipId: Int)
    abstract fun deleteMembership(membershipId: Int)
}

interface MembershipServiceCallback : DefaultServiceCallback {
    fun completeGetMembership(memberships: List<Membership>) {}
    fun completeAcceptMembership(membershipId: Int) {}
    fun completeDeclineMembership(membershipId: Int) {}
    fun completeAcceptPromotion(membershipId: Int) {}
    fun completeDeclinePromotion(membershipId: Int) {}
    fun completeDeleteMembership(membershipId: Int)

    fun errorGetMembership(reason: String) {}
    fun errorAcceptMembership(membershipId: Int) {}
    fun errorDeclineMembership(membershipId: Int) {}
    fun errorAcceptPromotion(membershipId: Int) {}
    fun errorDeclinePromotion(membershipId: Int) {}
    fun errorDeleteMembership(membershipId: Int)

}

class DefaultMembershipService(
    private val application: TsuApplication,
    var callback: MembershipServiceCallback?
) : MembershipService() {

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String = "DefaultMembershipService"

    @Inject
    lateinit var membershipApi: MembershipApi

    @Inject
    lateinit var schedulers: RxSchedulers

    init {
        application.appComponent.inject(this)
    }

    override fun getMemberships() {

        compositeDisposable += membershipApi.getMyCommunities()
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        callback?.completeGetMembership(
                            result.memberships
                        )
                    },
                    onFailure = { errMsg ->
                        callback?.errorGetMembership(errMsg)
                    }
                )

            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error in getMemberships", err)
            })

    }

    override fun acceptPromotion(promotionId: Int) {
        compositeDisposable += membershipApi.acceptPromotion(promotionId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.completeAcceptPromotion(promotionId)
                    },
                    onFailure = { errMsg ->
                        callback?.errorAcceptPromotion(promotionId)
                    }
                )
            }, { err ->
                callback?.errorAcceptPromotion(promotionId)
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error in acceptPromotion", err)
            })
    }

    override fun declinePromotion(promotionId: Int) {
        compositeDisposable += membershipApi.declinePromotion(promotionId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.completeDeclinePromotion(promotionId)
                    },
                    onFailure = {
                        callback?.errorDeclinePromotion(promotionId)
                    }
                )
            }, { err ->
                callback?.errorDeclinePromotion(promotionId)
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error in declinePromotion", err)
            })
    }

    override fun acceptMembership(membershipId: Int) {
        compositeDisposable += membershipApi.acceptMembership(membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.completeAcceptMembership(membershipId)
                    },
                    onFailure = {
                        callback?.errorAcceptMembership(membershipId)
                    }
                )
            }, { err ->
                callback?.errorAcceptMembership(membershipId)
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error in acceptMembership", err)
            })
    }

    override fun declineMembership(membershipId: Int) {
        compositeDisposable += membershipApi.declineMembership(membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.completeDeclineMembership(membershipId)
                    },
                    onFailure = {
                        callback?.errorDeclineMembership(membershipId)
                    }
                )
            }, { err ->
                callback?.errorDeclineMembership(membershipId)
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error in declineMembership", err)
            })
    }

    override fun deleteMembership(membershipId: Int) {
        compositeDisposable += membershipApi.deleteMembership(membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.completeDeleteMembership(membershipId)
                    },
                    onFailure = {
                        callback?.errorDeleteMembership(membershipId)
                    }
                )
            }, { err ->
                callback?.errorDeleteMembership(membershipId)
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error in declineMembership", err)
            })
    }


}