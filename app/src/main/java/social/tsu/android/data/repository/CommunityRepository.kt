package social.tsu.android.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.Membership
import social.tsu.android.network.model.PendingPost
import social.tsu.android.service.CommunitiesService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class CommunityRepository @Inject constructor(private val communityService: CommunitiesService) {

    fun getPendingPosts(communityId: Int): LiveData<Data<List<PendingPost>>> {
        val loadState = MutableLiveData<Data<List<PendingPost>>>()
        loadState.value = Data.Loading()
        communityService.getPublishingRequests(
            communityId,
            object : ServiceCallback<List<PendingPost>> {
                override fun onSuccess(result: List<PendingPost>) {
                    loadState.value = Data.Success(result)
                }

                override fun onFailure(errMsg: String) {
                    loadState.value = Data.Error(Throwable(errMsg))
                }
            })
        return loadState
    }

    fun leave(membershipId: Int): LiveData<Data<Boolean>> {
        val loadState = MutableLiveData<Data<Boolean>>()
        loadState.value = Data.Loading()
        communityService.leave(membershipId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                loadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

    fun joinCommunity(community: Group): LiveData<Data<Boolean>> {
        val loadState = MutableLiveData<Data<Boolean>>()
        loadState.value = Data.Loading()
        communityService.joinCommunity(community.id, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                loadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

    fun acceptMembership(membershipId: Int): LiveData<Data<Boolean>> {
        val loadState = MutableLiveData<Data<Boolean>>()
        loadState.value = Data.Loading()
        communityService.acceptMembership(membershipId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                loadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

    fun loadCommunity(communityId: Int): LiveData<Data<Group>> {
        val loadState = MutableLiveData<Data<Group>>()
        loadState.value = Data.Loading()
        communityService.loadCommunityInfo(communityId, object : ServiceCallback<Group> {
            override fun onSuccess(result: Group) {
                loadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

    fun loadMembership(communityId: Int): LiveData<Data<List<Membership>>> {
        val loadState = MutableLiveData<Data<List<Membership>>>()
        loadState.value = Data.Loading()
        communityService.getMemberships(communityId, object : ServiceCallback<List<Membership>> {
            override fun onSuccess(result: List<Membership>) {
                loadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

}