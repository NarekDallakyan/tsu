package social.tsu.android.service

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.api.MembershipApi
import social.tsu.android.network.model.*
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

abstract class CommunitiesService : DefaultService() {

    abstract fun createCommunity(
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?,
        serviceCallback: ServiceCallback<Group>
    )

    abstract fun updateCommunity(
        communityId: Int,
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?,
        serviceCallback: ServiceCallback<Group>
    )

    abstract fun loadCommunityInfo(communityId: Int, serviceCallback: ServiceCallback<Group>)
    abstract fun loadSuggestedCommunities(serviceCallback: ServiceCallback<List<Group>>)
    abstract fun joinCommunity(groupId: Int, serviceCallback: ServiceCallback<Boolean>)
    abstract fun acceptMembership(membershipId: Int, serviceCallback: ServiceCallback<Boolean>)
    abstract fun getPublishingRequests(
        communityId: Int,
        serviceCallback: ServiceCallback<List<PendingPost>>
    )

    abstract fun getCommunityPendingRequests(
        communityId: Int,
        serviceCallback: ServiceCallback<List<PendingRequest>>
    )

    abstract fun getMemberships(
        communityId: Int,
        serviceCallback: ServiceCallback<List<Membership>>
    )

    abstract fun leave(membershipId: Int, serviceCallback: ServiceCallback<Boolean>)
    abstract fun deleteCommunity(communityId: Int, serviceCallback: ServiceCallback<Boolean>)
}

class DefaultCommunitiesService @Inject constructor(
    private val application: Application,
    private val communityApi: CommunityApi,
    private val membershipApi: MembershipApi,
    private val schedulers: RxSchedulers
) : CommunitiesService() {

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String = "DefaultCommunitiesService"

    override fun loadCommunityInfo(communityId: Int, serviceCallback: ServiceCallback<Group>) {
        compositeDisposable += communityApi.getCommunityInfo(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(application, it, object : ServiceCallback<CommunityResponse> {
                    override fun onSuccess(result: CommunityResponse) {
                        serviceCallback.onSuccess(result.group)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error creating community", err)
            })
    }

    override fun getMemberships(
        communityId: Int,
        serviceCallback: ServiceCallback<List<Membership>>
    ) {

        compositeDisposable += membershipApi.getMyCommunities()
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(application, it, object : ServiceCallback<MembershipResponse> {
                    override fun onSuccess(result: MembershipResponse) {
                        serviceCallback.onSuccess(result.memberships)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })

            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error in getMemberships", err)
            })

    }

    override fun createCommunity(
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?,
        serviceCallback: ServiceCallback<Group>
    ) {
        compositeDisposable += communityApi.createCommunity(
            CommunityPayload(
                Community(
                    name, description, topicId, moderation, visibility.serverValue, image
                )
            )
        ).subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseWithWrapper(
                    application,
                    it,
                    object : ServiceCallback<CommunityResponse> {
                        override fun onSuccess(result: CommunityResponse) {
                            serviceCallback.onSuccess(result.group)
                        }

                        override fun onFailure(errMsg: String) {
                            serviceCallback.onFailure(errMsg)
                        }
                    })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error creating community", err)
            })

    }

    override fun updateCommunity(
        communityId: Int,
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?,
        serviceCallback: ServiceCallback<Group>
    ) {
        compositeDisposable += communityApi.updateCommunity(
            communityId,
            CommunityPayload(
                Community(
                    name, description, topicId, moderation, visibility.serverValue, image
                )
            )
        )
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(application, it, object : ServiceCallback<CommunityResponse> {
                    override fun onSuccess(result: CommunityResponse) {
                        serviceCallback.onSuccess(result.group)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error loading suggested community", err)
            })
    }

    override fun deleteCommunity(communityId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += communityApi.deleteCommunity(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error deleting community", err)
            })
    }

    override fun loadSuggestedCommunities(serviceCallback: ServiceCallback<List<Group>>) {
        compositeDisposable += communityApi.getSuggestedCommunities()
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(application, it, object : ServiceCallback<CommunityListResponse> {
                    override fun onSuccess(result: CommunityListResponse) {
                        serviceCallback.onSuccess(result.groups)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error loading suggested community", err)
            })
    }

    override fun joinCommunity(groupId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += communityApi.joinCommunity(groupId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error joining community", err)
            })
    }

    override fun acceptMembership(membershipId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += communityApi.acceptMembership(membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error joining community", err)
            })
    }

    override fun getCommunityPendingRequests(
        communityId: Int,
        serviceCallback: ServiceCallback<List<PendingRequest>>
    ) {
        compositeDisposable += communityApi.getPendingRequests(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(application, it, object : ServiceCallback<CommunityPendings> {
                    override fun onSuccess(result: CommunityPendings) {
                        serviceCallback.onSuccess(result.pendingRequests)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error getting pending requests", err)
            })
    }

    override fun getPublishingRequests(
        communityId: Int,
        serviceCallback: ServiceCallback<List<PendingPost>>
    ) {
        compositeDisposable += communityApi.getPendingPosts(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(application, it, object : ServiceCallback<PendingPostsResponse> {
                    override fun onSuccess(result: PendingPostsResponse) {
                        serviceCallback.onSuccess(result.posts)
                    }

                    override fun onFailure(errMsg: String) {
                        serviceCallback.onFailure(errMsg)
                    }
                })
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }

    override fun leave(membershipId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += communityApi.leave(membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseResult(application, it, serviceCallback)
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }
}