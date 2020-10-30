package social.tsu.android.viewModel.account

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.CreateAccountApi
import social.tsu.android.network.model.AccountInfoUser
import social.tsu.android.network.model.UserProfile
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.*
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.model.EditAccountForm
import social.tsu.android.validation.EmailValidator
import social.tsu.android.validation.TextNotEmptyValidator
import social.tsu.android.validation.UsernameValidator
import javax.inject.Inject


class EditAccountViewModel @Inject constructor(
    private val application: Application,
    private val createAccountApi: CreateAccountApi,
    private val schedulers: RxSchedulers
) : ViewModel(), UserInfoServiceCallback , UserAccountDeleteCallback {

    val viewForm = EditAccountForm()
    val requiredFieldsLiveData = MutableLiveData<Boolean>()
    val editLiveData = MutableLiveData<Data<UserProfile>>()
    val accountDeleteLiveData = MutableLiveData<Boolean>()
    val validationLiveData = MutableLiveData<List<Pair<AccountFields, Boolean>>>()

    private val userInfoService: UserInfoService by lazy {
        DefaultUserInfoService(application as TsuApplication, this, this)
    }

    private val disposable = CompositeDisposable()

    private var currentUserInfo: UserProfile? = null

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        currentUserInfo = info
        viewForm.bind(info)
    }

    override fun didErrorWith(message: String) {
        viewForm.password = ""
        viewForm.showProgress.set(false)
        editLiveData.postValue(Data.Error(Throwable(message)))
    }

    override fun completedUpdateUserAccount(info: UserProfile?) {
        viewForm.showProgress.set(false)
        if (info != null) {
            currentUserInfo = info
            editLiveData.postValue(Data.Success(info))
        } else {
            editLiveData.postValue(Data.Error(Throwable()))
        }
    }

    override fun completeUserAccountDelete(boolean: Boolean) {
        accountDeleteLiveData.postValue(true)
        viewForm.showProgress.set(false)
    }

    override fun failedUserAccountDelete(message: String) {
        viewForm.password = ""
        viewForm.showProgress.set(false)
        accountDeleteLiveData.postValue(false)
    }

    fun fetchUserInfo() {
        val userId = AuthenticationHelper.currentUserId ?: return

        viewForm.bind(userInfoService.getCachedUserInfo(userId))
        userInfoService.getUserInfo(userId, true)
    }

    fun saveAccount() {
        val userId = currentUserInfo?.id ?: return

        val user = AccountInfoUser(
            userId,
            viewForm.firstName,
            viewForm.lastName,
            viewForm.email,
            viewForm.username,
            viewForm.password,
            viewForm.phoneNumber
        )

        viewForm.showProgress.set(true)

        userInfoService.updateAccountInfo(user)
    }

    fun deleteAccount() {
        val userId = AuthenticationHelper.currentUserId ?: return
        val accessToken = AuthenticationHelper.authToken ?: return

        viewForm.showProgress.set(true)
        userInfoService.deleteAccount(userId, viewForm.password)
    }


    fun validateForm(): Boolean {
        val requiredFilledResult = listOf(
            Pair(AccountFields.Username, TextNotEmptyValidator.validate(viewForm.username)),
            Pair(AccountFields.Email, TextNotEmptyValidator.validate(viewForm.email))
        )

        val requiredFilled = requiredFilledResult.map { it.second }.all { it }
        requiredFieldsLiveData.postValue(requiredFilled)
        if (!requiredFilled) {
            validationLiveData.postValue(requiredFilledResult)
            return false
        }

        val validationResult = listOf(
            Pair(AccountFields.Username, UsernameValidator.validate(viewForm.username)),
            Pair(AccountFields.Email, EmailValidator.validate(viewForm.email)),
            Pair(AccountFields.FirstName, TextNotEmptyValidator.validate(viewForm.firstName)),
            Pair(AccountFields.LastName, TextNotEmptyValidator.validate(viewForm.lastName))
        )

        validationLiveData.postValue(validationResult)

        return validationResult.map { pair ->
            pair.second
        }.all {
            it
        }
    }

    fun checkIfUsernameExists() {
        if (currentUserInfo?.username == viewForm.username) {
            viewForm.usernameError.set(null)
            return
        }
        if (!UsernameValidator.validate(viewForm.username)) {
            return
        }
        disposable += createAccountApi.checkIfUsernameUnique(viewForm.username)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe ({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        val unique = it.data.unique

                        if (unique) {
                            viewForm.usernameError.set(null)
                        } else {
                            viewForm.usernameError.set(R.string.signup_username_exists)
                        }
                    },
                    onFailure = {
                        viewForm.usernameError.set(R.string.generic_error_message)
                    }
                )

            },{
                it.printStackTrace()
            })
    }

    fun checkIfEmailExists() {
        if (currentUserInfo?.email == viewForm.email) {
            viewForm.emailError.set(null)
            return
        }
        if (!EmailValidator.validate(viewForm.email)) {
            //not valid
            return
        }
        disposable += createAccountApi.checkIfEmailIsUnique(viewForm.email)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe ({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        val unique = response.body()?.data?.unique ?: false

                        if (unique) {
                            viewForm.emailError.set(null)
                        } else {
                            viewForm.emailError.set(R.string.signup_username_exists)
                        }
                    },
                    onFailure = {
                        viewForm.emailError.set(R.string.generic_error_message)
                    }
                )

            },{
                it.printStackTrace()
            })
    }

    enum class AccountFields {
        Email, Password, Username, FirstName, LastName, PhoneNumber
    }

}