package social.tsu.android.ui.user_profile.friends

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import retrofit2.Response
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.UserApi
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.UserProfile
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.*
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.SingleLiveEvent
import javax.inject.Inject


private const val TAG = "UserFriendsViewModel"
private const val PAGE_COUNT = 100

class UserFriendsViewModel @Inject constructor(
    private val application: TsuApplication,
    private val userApi: UserApi,
    private val friendService: FriendService,
    private val schedulers: RxSchedulers
) : ViewModel(), FriendServiceCallback, UserInfoServiceCallback, BlockServiceCallback {

    private val _userListLiveData = MutableLiveData<Data<List<UserProfile>>>()
    val userListLiveData: LiveData<Data<List<UserProfile>>> = _userListLiveData

    private val lastListResult = ArrayList<UserProfile>()

   val requestErrorLiveData = SingleLiveEvent<String>()

    var userId: Int = UserProfile.NO_USER_ID
    var listType: UserFriendListType = UserFriendListType.FRIEND_LIST
    var hasMoreItems: Boolean = true
        private set

    private var lastPage = 1

    private val compositeDisposable = CompositeDisposable()

    private val userService: UserInfoService by lazy {
        DefaultUserInfoService(application, this)
    }

    private val blockService: BlockService by lazy {
        DefaultBlockService(application, this)
    }

    init {
        friendService.setFriendServiceCallback(this)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    override fun didCompleteFollowRequest(userId: Int, didFollow: Boolean) {
        userService.getUserInfo(userId, true)
    }

    override fun didCompleteFriendRequest(userId: Int) {
        userService.getUserInfo(userId, true)
    }

    override fun didErrorWith(message: String) {
        requestErrorLiveData.postValue(message)
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        if (info == null) return

        val indexOf = lastListResult.indexOfFirst { it.id == info.id }
        if (indexOf >= 0) {
            lastListResult.removeAt(indexOf)
            lastListResult.add(indexOf, info)
            _userListLiveData.postValue(Data.Success(lastListResult))
        }
    }

    override fun didCompleteUserBlock(userId: Int, message: String) {
        userService.getUserInfo(userId, true)

    }

    override fun didCompleteUserUnblockResponse(userId: Int, message: String) {
        userService.getUserInfo(userId, true)
    }

    override fun didFailToBlockUser(userId: Int, message: String) {
        requestErrorLiveData.postValue(message)
    }

    override fun didFailToUnblockUser(userId: Int, message: String) {
        requestErrorLiveData.postValue(message)
    }

    fun requestFriend(userProfile: UserProfile) {
        friendService.requestFriend(userProfile.id)
    }

    fun reloadList() {
        lastPage = 1
        lastListResult.clear()
        loadNextItems()
    }

    fun reloadItems(firstPosition: Int, lastPosition: Int) {
        val pageCount = lastPosition - firstPosition + 2
        val page = (lastPosition / pageCount) + 1
        if (userId < 0) {
            Log.w(TAG, "Can't load $listType. No user id")
            return
        }
        val request = when (listType) {
            UserFriendListType.FRIEND_LIST -> userApi.getUserFriends(userId, page, pageCount)
            UserFriendListType.FOLLOWER_LIST -> userApi.getUserFollowers(userId, page, pageCount)
            UserFriendListType.FOLLOWING_LIST -> userApi.getUserFollowings(userId, page, pageCount)
            else -> null
        } ?: return

        _userListLiveData.postValue(Data.Loading())
        compositeDisposable += request
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        updateList(result.data)
                        _userListLiveData.postValue(Data.Success(lastListResult))
                    },
                    onFailure = {
                        _userListLiveData.postValue(Data.Error(Throwable(it)))
                    }
                )
            }, { error ->
                Log.e(TAG, "Can't load user list", error)
                _userListLiveData.postValue(
                    Data.Error(
                        Throwable(
                            error.getNetworkCallErrorMessage(
                                application
                            )
                        )
                    )
                )
            })
    }

    fun loadNextItems() {
        if (userId < 0) {
            Log.w(TAG, "Can't load $listType. No user id")
            return
        }
        when (listType) {
            UserFriendListType.FRIEND_LIST -> {
                loadUserList(userApi.getUserFriends(userId, lastPage, PAGE_COUNT))
            }
            UserFriendListType.FOLLOWER_LIST -> {
                loadUserList(userApi.getUserFollowers(userId, lastPage, PAGE_COUNT))
            }
            UserFriendListType.FOLLOWING_LIST -> {
                loadUserList(userApi.getUserFollowings(userId, lastPage, PAGE_COUNT))
            }
        }
    }

    fun unfriendUser(userProfile: UserProfile) {
        friendService.deleteFriend(userProfile.id, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                if (result) {
                    removeUserFromList(userProfile)
                }
            }

            override fun onFailure(errMsg: String) {
                Log.e(TAG, "Can't unfriend user. $errMsg")
                requestErrorLiveData.postValue(errMsg)
            }
        })
    }

    fun toggleFollowingForUser(userProfile: UserProfile) {
        if (userProfile.isFollowing) {
            friendService.unfollowUser(userProfile.id)
        } else {
            friendService.followUser(userProfile.id)
        }
    }

    fun toggleBlockForUser(userProfile: UserProfile) {
        if (userProfile.isBlocked) {
            blockService.unblockUser(userProfile.id)
        } else {
            blockService.blockUser(userProfile.id)
        }
    }

    private fun removeUserFromList(userProfile: UserProfile) {
        _userListLiveData.value?.let { data ->
            if (data is Data.Success) {
                val list = ArrayList(data.data)
                val indexOf = list.indexOfFirst { it.id == userProfile.id }
                if (indexOf >= 0) {
                    list.removeAt(indexOf)
                    _userListLiveData.postValue(Data.Success(list))
                }
            }
        }
    }

    private fun loadUserList(request: Single<Response<DataWrapper<List<UserProfile>>>>) {
        _userListLiveData.postValue(Data.Loading())
        compositeDisposable += request
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        hasMoreItems = result.data.isNotEmpty()
                        if (hasMoreItems) {
                            lastPage++
                        }
                        updateList(result.data)
                        _userListLiveData.postValue(Data.Success(lastListResult))
                    },
                    onFailure = {
                        _userListLiveData.postValue(Data.Error(Throwable(it)))
                    }
                )
            }, { error ->
                Log.e(TAG, "Can't load user list", error)
                _userListLiveData.postValue(Data.Error(error))
            })
    }

    private fun updateList(list: List<UserProfile>) {
        list.forEach { user ->
            val idx = lastListResult.indexOfFirst { it.id == user.id }
            if (idx >= 0) {
                lastListResult.removeAt(idx)
                lastListResult.add(idx, user)
            } else {
                lastListResult.add(user)
            }
        }
    }

}