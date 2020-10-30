package social.tsu.android.ui.ext

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import com.jakewharton.rxbinding3.view.focusChanges
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import social.tsu.android.validation.ValidationReactor
import social.tsu.android.validation.Validator

fun TextInputEditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object: TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
    })
}

fun TextInputEditText.afterFocusLost(afterFocusLost: () -> Unit) {
    this.setOnFocusChangeListener { v, hasFocus ->
        if(!hasFocus){
            afterFocusLost.invoke()
        }
    }
}

fun TextInputEditText.addReactiveValidator(validator: Validator<String>, callback: ValidationReactor): Disposable {
    val disposable = CompositeDisposable()
    disposable += focusChanges().skipInitialValue().subscribe { isFocused ->
        if(!isFocused){
            callback.validationResult(validator.validate(text.toString()))
        }
    }
    disposable += textChanges().subscribe {
        callback.clearError()
    }
    return disposable
}

fun TextInputEditText.addReactiveTextChangedValidator(
    validator: Validator<String>,
    callback: ValidationReactor
): Disposable {
    val disposable = CompositeDisposable()
    disposable += textChanges().subscribe({
        val isValid = validator.validate(text.toString())
        if (isValid) {
            callback.clearError()
        }
        callback.validationResult(isValid)
    }, { err ->
        callback.validationResult(false)
    })

    return disposable
}

fun TextInputEditText.addReactivePairTextChangedValidator(
    otherText: EditText?,
    validator: Validator<Pair<String, String>>,
    callback: ValidationReactor
): Disposable {
    val disposable = CompositeDisposable()
    disposable += textChanges().subscribe({
        val isValid = validator.validate(text.toString() to otherText?.text.toString())
        if (isValid) {
            callback.clearError()
        }
        callback.validationResult(isValid)
    }, { err ->
        callback.validationResult(false)
    })

    return disposable
}