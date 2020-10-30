package social.tsu.android.ui.new_post.supports

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.UserProfile
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.handleApiCallError
import social.tsu.android.service.handleResponseWithWrapper
import social.tsu.android.ui.model.Data
import javax.inject.Inject


private const val TAG = "SupportsListViewModel"

class SupportsListViewModel @Inject constructor(
    private val application: TsuApplication,
    private val postApi: PostApi,
    private val schedulers: RxSchedulers
) : ViewModel() {

    var postId: Int = -1

    private val _userListLiveData = MutableLiveData<Data<List<UserProfile>>>()
    val userListLiveData: LiveData<Data<List<UserProfile>>> = _userListLiveData

    private val compositeDisposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun loadDataForPost() {
        if (postId <= 0) {
            _userListLiveData.postValue(Data.Error(Throwable(application.getString(R.string.general_error))))
            return
        }
        val callback = object : ServiceCallback<List<UserProfile>> {
            override fun onSuccess(result: List<UserProfile>) {
                _userListLiveData.postValue(Data.Success(result))
            }

            override fun onFailure(errMsg: String) {
                _userListLiveData.postValue(Data.Error(Throwable(errMsg)))
            }

        }
        compositeDisposable += postApi.getPostSupports(postId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.io())
            .subscribe({ response ->
                handleResponseWithWrapper(application, response, callback)
            }, { error ->
                Log.e(TAG, "Can't load supports", error)
                handleApiCallError(application, error, callback)
            })
    }

}