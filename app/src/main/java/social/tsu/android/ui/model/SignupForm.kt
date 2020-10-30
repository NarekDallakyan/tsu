package social.tsu.android.ui.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import social.tsu.android.BirthDate
import social.tsu.android.validation.*

class SignupForm: BaseObservable(){
    @Bindable
    var email = ""

    @Bindable
    var password = ""

    @Bindable
    var confirmPassword = ""

    @Bindable
    var firstName = ""

    @Bindable
    var lastName = ""

    @Bindable
    var username = ""

    @Bindable
    var date = ObservableField<BirthDate>()

    @Bindable
    var isTosAccepted = ObservableBoolean(false)

    var inviteId: Int? = null
    var hubspotId: Int? = null
    var oldEmail: String? = null

    @Bindable
    var invitedByUsername: String? = null

    var emailError = ObservableField<Int>()
    var passwordError = ObservableField<Int>()
    var firstNameError = ObservableField<Int>()
    var lastNameError = ObservableField<Int>()
    var usernameError = ObservableField<Int>()
    var dateError = ObservableField<Int>()
    var confirmPasswordError = ObservableField<Int>()
    var invitedByUserNameError = ObservableField<Int>()
    var isTosAcceptedError = ObservableField<Int>()

    var showProgress = ObservableField<Boolean>(false)

    @Bindable
    fun isValid(): Boolean {
        // notifyPropertyChanged(BR.valid)
        return EmailValidator.validate(email) &&
                PasswordValidator.validate(password) &&
                TextNotEmptyValidator.validate(firstName) &&
                TextNotEmptyValidator.validate(lastName) &&
                UsernameValidator.validate(username) &&
                DateValidator.validate(date.get()?.stringValue ?: "") &&
                isTosAccepted.get()
    }
}