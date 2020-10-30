package social.tsu.android.ui.new_post.likes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.api.FriendsApi
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.UserProfile
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.getNetworkCallErrorMessage
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.errorFromResponse
import javax.inject.Inject


private const val TAG = "'LikesListViewModel'"

class LikesListViewModel @Inject constructor(
    private val application: TsuApplication,
    private val postApi: PostApi,
    private val friendsApi: FriendsApi,
    private val schedulers: RxSchedulers,
    private val moshi: Moshi
) : ViewModel() {

    var postId: Int = -1

    private val _userListLiveData = MutableLiveData<Data<List<UserProfile>>>()
    val userListLiveData: LiveData<Data<List<UserProfile>>> = _userListLiveData

    private val _errorMessageLiveData = MutableLiveData<String>()
    val errorMessageLiveData: LiveData<String> = _errorMessageLiveData

    private val compositeDisposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun loadDataForPost() {
        if (postId <= 0) {
            _errorMessageLiveData.postValue("No post id")
            return
        }
        compositeDisposable += postApi.getPostLikes(postId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.io())
            .subscribe({ response ->
                val data = response.body()?.data
                if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE && data != null) {
                    _userListLiveData.postValue(Data.Success(data))
                } else {
                    val error = moshi.errorFromResponse(response)
                    Log.e(TAG, "Can't load likes. ${error?.message}")
                    if (error != null) {
                        _userListLiveData.postValue(Data.Error(Throwable(error.message)))
                    } else {
                        _userListLiveData.postValue(Data.Error(Throwable()))
                    }
                }
            }, { error ->
                Log.e(TAG, "Can't load likes", error)
                _userListLiveData.postValue(Data.Error(error))
                _errorMessageLiveData.postValue(error.message)
            })
    }

    fun followUser(userProfile: UserProfile) {
        compositeDisposable += friendsApi.followUserId(userProfile.id)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.io())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        loadDataForPost()
                        _errorMessageLiveData.postValue(null)
                    },
                    onFailure = { errMsg ->
                        _errorMessageLiveData.postValue(errMsg)
                    }
                )

            }, { error ->
                Log.e(TAG, "Can't follow like user", error)
                _errorMessageLiveData.postValue(error.getNetworkCallErrorMessage(application))
            })
    }

}