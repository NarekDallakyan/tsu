package social.tsu.android.validation

import social.tsu.android.BirthDate
import social.tsu.android.R

val ensureNotBlank: (String, String?) -> ValidationResult = { s: String, _->
    if(s.isNotEmpty()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(R.string.signup_empty_field_validation_error)
    }
}

val ensureValidEmail: (String, String?) -> ValidationResult = { s: String, _ ->
    val isValid = EmailValidator.validate(s)
    if (isValid) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(R.string.signup_email_validation_error)
    }
}

val ensureOldEnough: (BirthDate, String?) -> ValidationResult = { bd: BirthDate, _ ->
    if (bd.isAllowedAge()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(R.string.signup_birthdate_validation_error)
    }
}

val ensureValidPassword: (String, String?) -> ValidationResult = { password: String, _ ->
    val isValid = PasswordValidator.validate(password)
    if (isValid) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(R.string.signup_password_validation_error)
    }
}

val ensureEqualPasswords: (Pair<String,String?>, String?) -> ValidationResult = { passwords: Pair<String,String?>, _ ->
    val notBlankResults = ensureNotBlank(passwords.first,null)
    if (notBlankResults is ValidationResult.Invalid){
        notBlankResults
    } else {
        if (passwords.second==null || passwords.first == passwords.second) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(R.string.signup_passwords_not_match_error)
        }
    }
}

