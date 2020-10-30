package social.tsu.android.validation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import social.tsu.android.utils.setErrorResource

object ValidatePassword: ValidatedField<Pair<EditText,String?>, TextInputLayout, Pair<String,String?> > {

    override fun validateField(
        valueComponent: Pair<EditText,String?>,
        errorComponent: TextInputLayout,
        predicate: (value: Pair<String, String?>, message: String?) -> ValidationResult
    ): Boolean {

        val result = predicate(Pair(valueComponent.first.text.toString(), valueComponent.second), null)
        handleResult(result, valueComponent, errorComponent)
        return result !is ValidationResult.Valid

    }

    override fun handleResult(
        result: ValidationResult,
        valueComponent: Pair<EditText,String?>,
        errorComponent: TextInputLayout
    ) {
        when(result) {
            is ValidationResult.Invalid -> errorComponent.setErrorResource(result.messageRes)
            is ValidationResult.Valid -> errorComponent.error = null
        }
    }
}

