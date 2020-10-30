package social.tsu.android.validation

import androidx.annotation.StringRes

sealed class ValidationResult {
    object Valid: ValidationResult()
    data class Invalid(@StringRes val messageRes: Int): ValidationResult()
}

