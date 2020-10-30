package social.tsu.android.ui.user_profile

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import social.tsu.android.*
import social.tsu.android.data.local.dao.TsuNotificationDao
import social.tsu.android.data.local.models.TsuNotificationType
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserFriendshipStatus
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.*
import social.tsu.android.ui.BlockCallback
import social.tsu.android.utils.SingleLiveEvent
import javax.inject.Inject


class UserProfileViewModel @Inject constructor(
    private val application: TsuApplication
) : ViewModel(), FriendServiceCallback, UserInfoServiceCallback,
    BlockServiceCallback, BlockCallback, ReportServiceCallback {

    private val _userProfileLiveData = MutableLiveData<UserProfile>()
    val userProfileLiveData: LiveData<UserProfile> = _userProfileLiveData
    val isBlock = MutableLiveData<Boolean>()
    private val _currentFriendStatus = MutableLiveData<UserFriendshipStatus>()
    val currentFriendStatus: LiveData<UserFriendshipStatus> = _currentFriendStatus

    private val _isFollowingLiveData = MutableLiveData<Boolean>()
    val isFollowingLiveData: LiveData<Boolean> = _isFollowingLiveData

    val messageLiveData = SingleLiveEvent<String>()

    var isUserBlocked: Boolean = false
        private set

    val userId: Int
        get() {
            return userProfileLiveData.value?.id ?: UserProfile.NO_USER_ID
        }

    val firstName: String?
        get() {
            return userProfileLiveData.value?.firstname
        }

    @Inject
    lateinit var schedulers: RxSchedulers

    val userService: UserInfoService by lazy {
        DefaultUserInfoService(application, this)
    }

    val reportService: ReportService by lazy {
        DefaultReportService(application, this)
    }

    @Inject
    lateinit var friendService: FriendService

    @Inject
    lateinit var tsuNotificationDao: TsuNotificationDao

    private val blockService: BlockService by lazy {
        DefaultBlockService(application, this)
    }

    init {
        _userProfileLiveData.observeOnce {
            if (it.id != AuthenticationHelper.currentUserId) {
                _isFollowingLiveData.postValue(it.isFollowing)
                _currentFriendStatus.postValue(it.userFriendshipStatus)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        blockService.onDestroy()
        friendService.onDestroy()
        userService.onDestroy()
    }

    override fun onBlocked(userId: Int) {
        if (this.userId == userId) {
            reloadUserInfo(userId)
        }
    }

    override fun onUnblocked(userId: Int) {
        if (this.userId == userId) {
            reloadUserInfo(userId)
        }
    }

    override fun didCompleteFollowRequest(userId: Int, didFollow: Boolean) {
        _isFollowingLiveData.postValue(didFollow)
        reloadUserInfo(userId)
    }

    override fun didGetFriendRequests() {
        userService.getUserInfo(userId, true)
    }

    override fun didCompleteFriendRequest(userId: Int) {
        _currentFriendStatus.postValue(UserFriendshipStatus.PENDING)
    }

    override fun didCompleteUserBlock(userId: Int, message: String) {
        reloadUserInfo(userId)
        snack(message)
    }

    override fun didCompleteUserUnblockResponse(userId: Int, message: String) {
        reloadUserInfo(userId)
        snack(message)
    }

    override fun didFailToBlockUser(userId: Int, message: String) {
        snack(message)
    }

    override fun didFailToUnblockUser(userId: Int, message: String) {
        snack(message)
    }

    override fun didCompleteUserReport(userId: Int, message: String) {
        snack(message)
    }

    override fun didFailToReportUser(userId: Int, message: String) {
        snack(message)
    }

    override fun didErrorWith(message: String) {
        snack(message)
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        if (info != null) {
            _userProfileLiveData.postValue(info)
            isUserBlocked = info.isBlocked
            isBlock.value = info.isBlocked
        } else {
            isUserBlocked = true
            isBlock.value = true
        }
    }

    fun toggleFollow() {
        val info = userProfileLiveData.value ?: return

        friendService.callback = this
        if (info.isFollowing) {
            friendService.unfollowUser(userId)
        } else {
            friendService.followUser(userId)
        }
    }

    fun toggleFriendship() {
        val info = userProfileLiveData.value ?: return
        friendService.callback = this

        when (currentFriendStatus.value) {
            UserFriendshipStatus.UNKNOWN -> {
                if (!info.isBlocked) {
                    friendService.requestFriend(userId)
                } else {
                    snack(R.string.user_is_blocked)
                }
            }
            UserFriendshipStatus.PENDING -> {
                friendService.cancelFriend(userId)
            }
            UserFriendshipStatus.ACCEPTED -> {
                friendService.deleteFriend(userId, object : ServiceCallback<Boolean> {
                    override fun onSuccess(result: Boolean) {
                        if (result) {
                            _currentFriendStatus.postValue(UserFriendshipStatus.UNKNOWN)
                        } else {
                            snack(R.string.general_error)
                        }
                    }

                    override fun onFailure(errMsg: String) {
                        snack(errMsg)
                    }
                })
            }
            UserFriendshipStatus.REQUESTED -> {
                if (!info.isBlocked) {
                    friendService.acceptFriend(userId, object : ServiceCallback<Boolean> {
                        override fun onSuccess(result: Boolean) {
                            if (result) {
                                execute {
                                    tsuNotificationDao.deleteNotification(
                                        userId,
                                        TsuNotificationType.NEW_FRIEND_REQUEST
                                    )
                                }
                                _currentFriendStatus.postValue(UserFriendshipStatus.ACCEPTED)
                            } else {
                                snack(R.string.general_error)
                            }
                        }

                        override fun onFailure(errMsg: String) {
                            snack(errMsg)
                        }
                    })
                } else {
                    snack(R.string.user_is_blocked)
                }
            }
        }
    }

    fun reloadUserInfo(userId: Int) {
        userService.getUserInfo(userId, true)
    }

    fun loadTagUser(tagUser:String){
        userService.getUserTagInfo(tagUser , true)
    }

    fun toggleUserBlock(userId: Int, blocked: Boolean) {
        if (blocked) {
            blockService.unblockUser(userId)
        } else {
            blockService.blockUser(userId)
        }
    }

    fun reportUser(userId: Int, type: Int) {
        reportService.reportUser(userId, type)
    }

    private fun snack(message: String) {
        messageLiveData.postValue(message)
    }

    private fun snack(@StringRes messageRes: Int) {
        messageLiveData.postValue(application.getString(messageRes))
    }
}