package social.tsu.android.service

import android.util.Log
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.api.MembershipApi
import social.tsu.android.network.model.CommunityMember
import social.tsu.android.network.model.MemberKickRequest
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


interface CommunityMembersServiceCallback : DefaultServiceCallback {
    fun didLoadCommunityMembers(members: List<CommunityMember>)
    fun didMemberKicked(member:CommunityMember)
    fun didMemberPromoted(member:CommunityMember)
    fun didMemberDemoted(member:CommunityMember)
}

abstract class CommunityMembersService : DefaultService() {

    abstract var callback: CommunityMembersServiceCallback?

    abstract fun loadMemberList(communityId: Int, page: Int, count: Int)
    abstract fun kickMember(communityId: Int, member: CommunityMember)
    abstract fun promoteMember(member: CommunityMember)
    abstract fun demoteMember(member: CommunityMember)

}

class DefaultCommunityMembersService(
    private val application: TsuApplication
) : CommunityMembersService() {

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String = "DefaultCommunityMembersService"

    override var callback: CommunityMembersServiceCallback? = null

    @Inject
    lateinit var communityApi: CommunityApi

    @Inject
    lateinit var membershipApi: MembershipApi

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var moshi: Moshi

    init {
        application.appComponent.inject(this)
    }

    override fun loadMemberList(communityId: Int, page: Int, count: Int) {
        compositeDisposable += communityApi.getCommunityMembers(communityId, page, count)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        callback?.didLoadCommunityMembers(result.members)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )

            }, { error ->
                callback?.didErrorWith(error.getNetworkCallErrorMessage(application))
                Log.e(tag, "Can't load group $communityId members", error)
            })
    }

    override fun kickMember(communityId: Int, member: CommunityMember) {
        compositeDisposable += communityApi.kickMember(communityId, MemberKickRequest(member.id))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        callback?.didMemberKicked(member)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { error ->
                callback?.didErrorWith(error.getNetworkCallErrorMessage(application))
                Log.e(tag, "Can't kick member ${member.fullname}", error)
            })
    }

    override fun promoteMember(member: CommunityMember) {
        compositeDisposable += membershipApi.promoteMembership(member.membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        callback?.didMemberPromoted(member)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { error ->
                callback?.didErrorWith(error.getNetworkCallErrorMessage(application))
                Log.e(tag, "Can't promote member ${member.fullname}", error)
            })
    }

    override fun demoteMember(member: CommunityMember) {
        compositeDisposable += membershipApi.demoteMembership(member.membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        callback?.didMemberDemoted(member)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { error ->
                callback?.didErrorWith(error.getNetworkCallErrorMessage(application))
                Log.e(tag, "Can't demote member ${member.fullname}", error)
            })
    }

}