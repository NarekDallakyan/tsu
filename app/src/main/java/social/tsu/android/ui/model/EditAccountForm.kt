package social.tsu.android.ui.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import social.tsu.android.network.model.UserProfile


class EditAccountForm: BaseObservable() {

    @Bindable
    var username = ""
    @Bindable
    var email = ""
    @Bindable
    var password = ""
    @Bindable
    var firstName = ""
    @Bindable
    var lastName = ""
    @Bindable
    var phoneNumber: String? = null
        get() {
            if (field?.isBlank() == true) return null
            return field
        }

    val emailError = ValidationObservable()
    val firstNameError = ValidationObservable()
    val lastNameError = ValidationObservable()
    val usernameError = ValidationObservable()
    val phoneNumberError = ValidationObservable()

    val showProgress = ObservableBoolean()

    val isValid = object : ObservableBoolean() {
        override fun get(): Boolean {
            return emailError.isValid()
                    && usernameError.isValid()
                    && firstNameError.isValid()
                    && lastNameError.isValid()
                    && phoneNumberError.isValid()
        }
    }

    fun bind(userInfo: UserProfile?) {
        username = userInfo?.username ?: ""
        email = userInfo?.email ?: ""
        firstName = userInfo?.firstname ?: ""
        lastName = userInfo?.lastname ?: ""
        phoneNumber = userInfo?.phoneNumber
        notifyChange()
    }

    inner class ValidationObservable: ObservableField<Int>() {
        override fun set(value: Int?) {
            super.set(value)
            isValid.notifyChange()
        }

        fun isValid(): Boolean {
            val value = get()
            return value == null || value == 0
        }
    }

}