package social.tsu.android.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.ReferralApi
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class InviteFriendsViewModel @Inject constructor(
    private val application: TsuApplication,
    private val schedulers: RxSchedulers,
    private val referralApi: ReferralApi
) : ViewModel() {
    private val _inviteLink = MutableLiveData<Data<String>>()

    private val compositeDisposable = CompositeDisposable()

    val inviteLink: LiveData<Data<String>>
        get() = _inviteLink

    fun fetchInviteLink() {
        compositeDisposable += referralApi.getReferralLink()
            .subscribeOn(schedulers.io())
            .doOnSubscribe {
                _inviteLink.postValue(Data.Loading())
            }
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        _inviteLink.postValue(Data.Success(result.data.referralLink))
                    },
                    onFailure = {
                        _inviteLink.postValue(Data.Error(Throwable(it)))
                    }
                )
            }, {
                _inviteLink.postValue(Data.Error(it))
            })
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}
