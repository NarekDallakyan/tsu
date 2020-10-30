package social.tsu.android.validation

import com.google.android.material.textfield.TextInputLayout
import social.tsu.android.BirthDate
import social.tsu.android.utils.setErrorResource

object ValidateBirthDate: ValidatedField<BirthDate, TextInputLayout, BirthDate> {
    override fun validateField(
        valueComponent: BirthDate,
        errorComponent: TextInputLayout,
        predicate: (value: BirthDate, message: String?) -> ValidationResult
    ): Boolean {

        val result = predicate(valueComponent, null)
        handleResult(result, valueComponent, errorComponent)
        return result !is ValidationResult.Valid
    }

    override fun handleResult(
        result: ValidationResult,
        valueComponent: BirthDate,
        errorComponent: TextInputLayout
    ) {
        when(result) {
            is ValidationResult.Invalid -> errorComponent.setErrorResource(result.messageRes)
            is ValidationResult.Valid -> errorComponent.error = null
        }
    }
}

