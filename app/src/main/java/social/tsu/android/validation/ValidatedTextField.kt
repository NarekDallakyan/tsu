package social.tsu.android.validation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import social.tsu.android.utils.setErrorResource

object ValidatedTextField:
    ValidatedField<EditText, TextInputLayout, String> {
    override fun handleResult(
        result: ValidationResult,
        valueComponent: EditText,
        errorComponent: TextInputLayout
    ) {
        when(result) {
            is ValidationResult.Invalid -> errorComponent.setErrorResource(result.messageRes)
            is ValidationResult.Valid -> errorComponent.error = null
        }

    }

    override fun validateField(
        valueComponent: EditText,
        errorComponent: TextInputLayout,
        predicate: (value: String, message: String?) -> ValidationResult
    ): Boolean {
        val result = predicate(valueComponent.text.toString(), null)
        handleResult(result, valueComponent, errorComponent)
        return result !is ValidationResult.Valid
    }
}

