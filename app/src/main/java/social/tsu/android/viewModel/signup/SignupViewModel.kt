package social.tsu.android.viewModel.signup

import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.BirthDate
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.DeviceUtils
import social.tsu.android.network.api.CreateAccountApi
import social.tsu.android.network.model.CreateAccountRequest
import social.tsu.android.network.model.CreateAccountResponsePayload
import social.tsu.android.network.model.User
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.getNetworkCallErrorMessage
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.model.SignupForm
import social.tsu.android.utils.extractUsername
import social.tsu.android.validation.*
import javax.inject.Inject

class SignupViewModel @Inject constructor(
    private val application: TsuApplication,
    private val createAccountAPI: CreateAccountApi,
    private val moshi: Moshi,
    private val schedulers: RxSchedulers
): ViewModel() {

    var signupForm: SignupForm = SignupForm()

    val validationLiveData: MutableLiveData<List<Pair<SignupFields, Boolean>>> = MutableLiveData<List<Pair<SignupFields, Boolean>>>()

    var createUserLiveData = MutableLiveData<Data<CreateAccountResponsePayload>>()
        private set

    var birthdayLiveData = MediatorLiveData<BirthDate>()
        private set

    var tosLiveData = MediatorLiveData<Boolean>()
        private set

    val tosAccepted = ObservableBoolean(false)

    private val disposable = CompositeDisposable()

    init{
        signupForm.isTosAccepted.addOnPropertyChangedCallback(object: OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                tosLiveData.value = signupForm.isTosAccepted.get()
            }
        })
    }

    fun createUser() {

        if (!validateForm()) {
            return
        }

        val acceptTos = if (signupForm.isTosAccepted.get()) "true" else "false"

        val user2 = User(
            signupForm.firstName,
            signupForm.lastName,
            signupForm.email,
            signupForm.username,
            acceptTos,
            signupForm.password,
            invitedById = signupForm.inviteId,
            hubspotId = signupForm.hubspotId,
            invitedByUsername = extractUsername(signupForm.invitedByUsername)

        )


        //If birthday is not set, form validation should handle it above
        val birthday = signupForm.date.get() ?: return

        signupForm.showProgress.set(true)

        val createAcctRequest = CreateAccountRequest(
            user2,
            DeviceUtils.getDeviceId(), birthday.dayString,
            birthday.monthString, birthday.yearString, acceptTos,
            signupForm.oldEmail
        )

        disposable += createAccountAPI.createAccount(createAcctRequest)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({response ->
                Log.d("API", "GOOD!!!!!")

                signupForm.showProgress.set(false)

                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        if (result.data == null) {
                            createUserLiveData.postValue(
                                Data.Error(
                                    Throwable(
                                        application.getString(
                                            R.string.generic_error_message
                                        )
                                    )
                                )
                            )
                            return@handleResponse
                        }
                        AuthenticationHelper.update(result.data)

                        createUserLiveData.postValue(Data.Success(result.data))
                    },
                    onFailure = { errMsg ->
                        createUserLiveData.postValue(Data.Error(Throwable(errMsg)))
                    }
                )

            }, { t ->
                Log.d("API", "Failed!!!!!", t)
                signupForm.showProgress.set(false)
                createUserLiveData.postValue(
                    Data.Error(
                        Throwable(
                            t.getNetworkCallErrorMessage(
                                application
                            )
                        )
                    )
                )
            })
        //signupForm.showProgress.set(false)
    }

    private fun validateForm(): Boolean {
        val validationResult = listOf(
            Pair(SignupFields.Email, EmailValidator.validate(signupForm.email)),
            Pair(SignupFields.Password, PasswordValidator.validate(signupForm.password)),
            Pair(SignupFields.Username, UsernameValidator.validate(signupForm.username)),
            Pair(SignupFields.Firstname, TextNotEmptyValidator.validate(signupForm.firstName)),
            Pair(SignupFields.Lastname, TextNotEmptyValidator.validate(signupForm.lastName)),
            Pair(
                SignupFields.Date,
                DateValidator.validate(signupForm.date.get()?.stringValue ?: "")
            ),
            Pair(
                SignupFields.ConfirmPassword,
                EqualPasswordsValidator.validate(signupForm.password to signupForm.confirmPassword)
            ),
            Pair(
                SignupFields.Date,
                DateValidator.validate(signupForm.date.get()?.stringValue ?: "")
            ),
            if (signupForm.invitedByUsername != null) {
                Pair(
                    SignupFields.InvitedByUsername,
                    InvitedByUsernameValidator.validate(signupForm.invitedByUsername ?: "")
                )
            } else {
                Pair(SignupFields.InvitedByUsername, true)
            },
            Pair(SignupFields.TosAccepted, signupForm.isTosAccepted.get())
        )

        validationLiveData.postValue(validationResult)

        return validationResult.map { pair ->
            Boolean
            pair.second
        }.all {
            it
        }
    }

    fun checkIfUsernameExists() {
        verifyIfUsernameExists(signupForm.username) {
            if (it) {
                signupForm.usernameError.set(0)
            } else {
                signupForm.usernameError.set(R.string.signup_username_exists)
            }
        }

    }

    fun checkIfInvitedByUsernameExists(invitedByUsername: String) {
        verifyIfUsernameExists(invitedByUsername) {
            if (it) {
                signupForm.invitedByUserNameError.set(R.string.invited_by_username_doesnt_exist)
            } else {
                signupForm.invitedByUserNameError.set(0)
            }
        }

    }

    private fun verifyIfUsernameExists(username: String, resultHandler: (unique: Boolean) -> Unit) {
        if (!UsernameValidator.validate(username)) {
            return
        }
        disposable += createAccountAPI.checkIfUsernameUnique(username)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe ({ response ->

                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        val unique = it.data.unique

                        resultHandler.invoke(unique)
                    },
                    onFailure = {
                        resultHandler.invoke(false)
                    }
                )
            },{
                it.printStackTrace()
            })

    }

    fun checkIfEmailExists() {
        if (!EmailValidator.validate(signupForm.email)) {
            //not valid
            return
        }
        disposable += createAccountAPI.checkIfEmailIsUnique(signupForm.email)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe ({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        val unique = it.data.unique

                        if (unique) {
                            signupForm.emailError.set(0)
                        } else {
                            signupForm.emailError.set(R.string.signup_username_exists)
                        }
                    },
                    onFailure = {
                        signupForm.emailError.set(R.string.generic_error_message)
                    }
                )

            },{
                it.printStackTrace()
            })

    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

    enum class SignupFields {
        Email, Password, ConfirmPassword, Username, Firstname, Lastname, TosAccepted, Date, InvitedByUsername
    }

}