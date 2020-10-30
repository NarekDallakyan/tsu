package social.tsu.android.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONObject
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.AccountApi
import social.tsu.android.network.model.Account
import social.tsu.android.network.model.TsuMessage
import social.tsu.android.network.model.UserProfile
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.*
import social.tsu.android.ui.model.Data
import javax.inject.Inject


private const val TAG = "BankAccountViewModel"

class BankAccountViewModel @Inject constructor(
    private val application: TsuApplication,
    private val schedulers: RxSchedulers,
    private val accountApi: AccountApi,
    private val moshi: Moshi
) : ViewModel(), UserInfoServiceCallback {

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    private val payPalValid = MutableLiveData<Data<Any>>()
    private val compositeDisposable = CompositeDisposable()
    private val balance = MutableLiveData<Data<Account>>()

    private val _redeemResponse = MutableLiveData<Data<TsuMessage>>()

    val redeemResponse
        get() = _redeemResponse as LiveData<Data<TsuMessage>>

    private val _userProfileLiveData = MutableLiveData<UserProfile>()
    val userProfileLiveData: LiveData<UserProfile> = _userProfileLiveData

    private val userService: UserInfoService by lazy {
        DefaultUserInfoService(application, this)
    }

    init {
        Log.d(TAG, "I've been init")

        val userId = AuthenticationHelper.currentUserId
        if (userId != null) {
            userService.getUserInfo(userId, false)
        } else {
            Log.w(TAG, "Can't load user info")
        }
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        info?.let { _userProfileLiveData.postValue(it) }
    }

    override fun didErrorWith(message: String) {
    }

    fun fetchAccountBalance(): LiveData<Data<Account>> {
        compositeDisposable += accountApi.getAccountBalance()
            .subscribeOn(schedulers.io())
            .doOnSubscribe {
                balance.postValue(Data.Loading())
            }
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        balance.postValue(Data.Success(result.data))
                    },
                    onFailure = { errMsg ->
                        Log.e(TAG, "Error: $errMsg ")
                        balance.postValue(Data.Error(Throwable(errMsg)))
                    }
                )
            }, {
                Log.e(TAG, "Error ", it)
                balance.postValue(Data.Error(Throwable(it.getNetworkCallErrorMessage(application))))
            })

        return balance
    }

    fun validateResponse(payload: JSONObject): LiveData<Data<Any>> {
        val formattedPayload =
            moshi.adapter(Any::class.java).fromJson(payload.toString())

        compositeDisposable += accountApi.validatePayPal( formattedPayload)
            .subscribeOn(schedulers.io())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        payPalValid.postValue(Data.Success(result.data))
                    },
                    onFailure = { errMsg ->
                        Log.e(TAG, "Error: $errMsg ")
                        payPalValid.postValue(Data.Error(Throwable(errMsg)))
                    }
                )
            }, {
                Log.e(TAG, "Error ", it)
                payPalValid.postValue(Data.Error(Throwable(it.getNetworkCallErrorMessage(application))))
            })

        return payPalValid
    }

    fun requestRedemption(amount: Double) {
        if (amount >= sharedPrefManager.getMinRedeemBalanceValue()) {
            compositeDisposable += accountApi.requestAccountRedemption(amount)
                .subscribeOn(schedulers.io())
                .subscribe({
                    handleResponse(
                        application,
                        it,
                        onSuccess = { result ->
                            _redeemResponse.postValue(Data.Success(result))
                        },
                        onFailure = { errMsg ->
                            Log.e(TAG, "Error: $errMsg ")
                            _redeemResponse.postValue(Data.Error(Throwable(errMsg)))
                        }
                    )
                }, {
                    _redeemResponse.postValue(Data.Error(it))
                })
        }
    }


    override fun onCleared() {
        Log.d(TAG, "Cleanup")
        compositeDisposable.dispose()
        super.onCleared()
    }
}