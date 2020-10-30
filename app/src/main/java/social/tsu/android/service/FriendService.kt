package social.tsu.android.service

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers

import social.tsu.android.network.api.FriendRequestType
import social.tsu.android.network.api.FriendsApi
import social.tsu.android.network.model.FriendRequest
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

interface FriendServiceCallback: DefaultServiceCallback {
    fun didGetFriendRequests() {}
    fun didCompleteFriendRequest(userId: Int) {}
    fun didCompleteFollowRequest(userId: Int, didFollow: Boolean) {}
}

abstract class FriendService: DefaultService() {
    abstract var friendRequests: MutableList<FriendRequest>
    abstract var callback: FriendServiceCallback?

    abstract fun getFriendRequests()
    abstract fun followUser(userId: Int)
    abstract fun unfollowUser(userId: Int)
    abstract fun requestFriend(userId: Int)
    abstract fun acceptFriend(userId: Int, serviceCallback: ServiceCallback<Boolean>)
    abstract fun declineFriend(userId: Int, serviceCallback: ServiceCallback<Boolean>)
    abstract fun deleteFriend(userId: Int, serviceCallback: ServiceCallback<Boolean>)
    abstract fun cancelFriend(userId: Int)
    abstract fun setFriendServiceCallback(friendServiceCallback: FriendServiceCallback)
}

class DefaultFriendService @Inject constructor(
    private val application: Application,
    private val friendsApi: FriendsApi,
    private val schedulers: RxSchedulers
): FriendService() {

    override var friendRequests: MutableList<FriendRequest> = mutableListOf()

    override var callback: FriendServiceCallback? = null

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()


    override val tag: String
        get() = "DefaultFriendService"

    override fun setFriendServiceCallback(friendServiceCallback: FriendServiceCallback) {
        callback = friendServiceCallback
    }

    override fun followUser(userId: Int) {

        compositeDisposable += friendsApi.followUserId(userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({

                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.didCompleteFollowRequest(userId, true)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith("Unable to follow. $errMsg}")
                    }
                )

            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e("USERPROFILE", "Error updating follow status", err)
            })
    }

    override fun unfollowUser(userId: Int) {
        compositeDisposable += friendsApi.unFollowUserId(userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({

                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.didCompleteFollowRequest(userId, false)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith("Unable to unfollow. $errMsg")
                    }
                )

            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e("USERPROFILE", "Error updating unfollow status", err)
            })
    }

    override fun getFriendRequests() {
        compositeDisposable += friendsApi.pendingFriendRequests()
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    Log.d(tag, response.body().toString())
                    handleResponse(
                        application,
                        response,
                        onSuccess = {
                            friendRequests.clear()
                            friendRequests.addAll(
                                response.body()?.data?.pending?.friendRequests ?: mutableListOf()
                            )
                            callback?.didGetFriendRequests()
                        }
                    )

                },
                { err ->
                    callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                    Log.d(tag, err.toString())
                }
            )
    }

    override fun requestFriend(userId: Int) {

        compositeDisposable += friendsApi.friendUserId(FriendRequestType.request.toString(), userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = {
                            callback?.didCompleteFriendRequest(userId)
                            getFriendRequests()
                        },
                        onFailure = { errMsg ->
                            callback?.didErrorWith("Unable to send friend request. $errMsg")
                        }
                    )
                    Log.d(tag, response.body().toString())

                },
                { err ->
                    Log.d(tag, err.toString())
                    callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                }
            )
    }

    override fun acceptFriend(userId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += friendsApi.friendUserId(FriendRequestType.accept.toString(), userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    handleResponseResult(application, response, serviceCallback)
                    getFriendRequests()
                },
                { err ->
                    Log.d(tag, err.toString())
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }

    override fun cancelFriend(userId: Int) {

        compositeDisposable += friendsApi.friendUserId(FriendRequestType.cancel.toString(), userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = {
                            Log.d(tag, response.body().toString())
                            callback?.didCompleteFriendRequest(userId)
                            getFriendRequests()
                        }
                    )
                },
                { err ->
                    Log.d(tag, err.toString())
                }
            )
    }

    override fun declineFriend(userId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += friendsApi.friendUserId(FriendRequestType.decline.toString(), userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    handleResponseResult(application, response, serviceCallback)
                    getFriendRequests()
                },
                { err ->
                    Log.d(tag, err.toString())
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }

    override fun deleteFriend(userId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += friendsApi.unFriendUserId(userId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponseResult(application, response, serviceCallback)
            }, { err ->
                Log.d(tag, err.toString())
                handleApiCallError(application, err, serviceCallback)
            })
    }

}