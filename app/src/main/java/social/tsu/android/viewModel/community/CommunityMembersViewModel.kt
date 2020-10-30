package social.tsu.android.viewModel.community

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.network.model.CommunityMember
import social.tsu.android.network.model.Role
import social.tsu.android.service.CommunityMembersService
import social.tsu.android.service.CommunityMembersServiceCallback
import social.tsu.android.utils.SingleLiveEvent
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


private const val PAGE_LIMIT = 30

class CommunityMembersViewModel @Inject constructor(
    private val application: TsuApplication,
    private val membersService: CommunityMembersService
) : ViewModel(), CommunityMembersServiceCallback, CoroutineScope {

    private val _isLoadingLiveData = MutableLiveData<Boolean>()
    val isLoadingLiveData: LiveData<Boolean> = _isLoadingLiveData

    private val _userListLiveData = MutableLiveData<List<Any>>()
    val userListLiveData: LiveData<List<Any>> = _userListLiveData
    private val lastLoaded = arrayListOf<Any>()

    val errorLiveData = SingleLiveEvent<String>()
    val serviceMessageLiveData = SingleLiveEvent<String>()

    var communityId = 0
    var userRole: Role = Role.MEMBER

    val isAllowedToEdit: Boolean
        get() = userRole == Role.OWNER || userRole == Role.ADMIN
    var hasMoreItems: Boolean = true
        private set
    val canLoadMore: Boolean
        get() {
            return hasMoreItems && isLoadingLiveData.value != true
        }

    private var lastPage = 1

    init {
        membersService.callback = this
    }

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.IO

    override fun onCleared() {
        super.onCleared()
        cancel()
    }

    private fun loadPage(page: Int) {
        if (communityId > 0) {
            _isLoadingLiveData.postValue(true)
            membersService.loadMemberList(communityId, page, PAGE_LIMIT)
        }
    }

    fun refresh() {
        hasMoreItems = true
        lastPage = 1
        lastLoaded.clear()
        loadPage(lastPage)
    }

    fun loadNextPage() {
        loadPage(++lastPage)
    }

    fun kickMember(member: CommunityMember) {
        if (communityId > 0) {
            membersService.kickMember(communityId, member)
        }
    }

    fun promoteMember(member: CommunityMember) {
        if (member.isAdmin) {
            membersService.demoteMember(member)
        } else {
            membersService.promoteMember(member)
        }
    }

    override fun didLoadCommunityMembers(members: List<CommunityMember>) {
        launch {
            synchronized(lastLoaded) {
                hasMoreItems = members.size >= PAGE_LIMIT

                if (lastLoaded.isEmpty()) {
                    lastLoaded += R.string.community_members_owner
                    lastLoaded += R.string.community_members_admins
                    lastLoaded += R.string.community_members_category
                }
                members.forEach { user ->
                    if (!lastLoaded.contains(user)) {
                        when {
                            user.isOwner -> lastLoaded.add(
                                lastLoaded.indexOf(R.string.community_members_admins),
                                user
                            )
                            user.isAdmin -> lastLoaded.add(
                                lastLoaded.indexOf(R.string.community_members_category),
                                user
                            )
                            else -> lastLoaded.add(user)
                        }
                    }
                }
                _userListLiveData.postValue(lastLoaded)
            }
            _isLoadingLiveData.postValue(false)
        }
    }

    override fun didErrorWith(message: String) {
        _isLoadingLiveData.postValue(false)
        if (message.isNotBlank()) {
            errorLiveData.postValue(message)
        } else {
            errorLiveData.postValue(application.getString(R.string.general_error))
        }
    }

    override fun didMemberDemoted(member: CommunityMember) {
        val message = application.getString(R.string.community_members_demoted_msg, member.fullname)
        serviceMessageLiveData.postValue(message)
        updateWithMember(member.copy(role = CommunityMember.ROLE_MEMBER))
    }

    override fun didMemberPromoted(member: CommunityMember) {
        val message =
            application.getString(R.string.community_members_promoted_msg, member.fullname)
        serviceMessageLiveData.postValue(message)
        updateWithMember(member.copy(status = 1))
    }

    override fun didMemberKicked(member: CommunityMember) {
        launch {
            val index = lastLoaded.indexOfFirst {
                it is CommunityMember && it.id == member.id
            }
            if (index >= 0) {
                lastLoaded.removeAt(index)
                if (this.isActive) _userListLiveData.postValue(lastLoaded)
            }
        }
    }

    private fun updateWithMember(member: CommunityMember) = launch {
        synchronized(lastLoaded) {
            val index = lastLoaded.indexOfFirst {
                it is CommunityMember && it.id == member.id
            }
            if (index >= 0) {
                lastLoaded.removeAt(index)
                lastLoaded.add(index, member)
                if (this.isActive) _userListLiveData.postValue(lastLoaded)
            }
        }
    }

}