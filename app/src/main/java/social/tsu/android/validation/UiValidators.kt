package social.tsu.android.validation

import android.widget.TextView
import androidx.databinding.ObservableField
import com.google.android.material.textfield.TextInputLayout
import social.tsu.android.AGE_LIMIT
import social.tsu.android.utils.extractUsername
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

interface Validator<in T> {
    fun validate(value: T): Boolean
}

interface PairTextValidator : Validator<Pair<String, String>> {
    override fun validate(value: Pair<String, String>): Boolean
}

interface TextValidator : Validator<String> {
    override fun validate(value: String): Boolean
}

interface BooleanValidator : Validator<Boolean> {
    override fun validate(value: Boolean): Boolean
}


interface ValidationReactor {
    fun validationResult(isValid: Boolean)
    fun clearError()
}

class TextInputLayoutReactor(val errorView: TextInputLayout, val errorText: String):
    ValidationReactor {
    override fun validationResult(isValid: Boolean) {
        errorView.error = if(isValid) null else errorText
    }

    override fun clearError() {
        errorView.error = null
    }

}

class ObservableFieldReactor<T>(
    private val errorField: ObservableField<T>,
    private val errorValue: T
): ValidationReactor {

    override fun validationResult(isValid: Boolean) {
        errorField.set(if (isValid) null else errorValue)
    }

    override fun clearError() {
        errorField.set(null)
    }
}

class TextViewReactor(val errorView: TextView, val errorText: String):
    ValidationReactor {
    override fun validationResult(isValid: Boolean) {
        errorView.text = if(isValid) null else errorText
    }

    override fun clearError() {
        errorView.text = ""
    }
}

object TextNotEmptyValidator : TextValidator {
    override fun validate(value: String): Boolean {
        return value.isNotEmpty()
    }
}

object PasswordValidator : TextValidator {
    override fun validate(value: String): Boolean {
        val matcher =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[#?!|@\$%^&*-.\\[\\]])(?=\\S+\$).{8,128}")
                .matcher(value)
        return value.isNotBlank() && matcher.matches()
    }
}


object EqualPasswordsValidator : PairTextValidator {
    override fun validate(value: Pair<String, String>): Boolean {
        return value.first == value.second
    }
}

object EmailValidator : TextValidator {
    override fun validate(value: String): Boolean {
        val matcher =
            Pattern.compile("^([\\w\\-.+_]+@(?:\\w[\\w\\-\\_]+\\.)+[\\w\\-]+)\$").matcher(value)
        return value.isNotBlank() && matcher.matches()
    }
}

object UsernameValidator : TextValidator {
    override fun validate(value: String): Boolean {
        val matcher = Pattern.compile("\\w{3,30}").matcher(value)
        return value.isNotBlank() && matcher.matches()
    }
}

object InvitedByUsernameValidator : TextValidator {
    override fun validate(value: String): Boolean {
        val matcher = Pattern.compile("\\w{3,30}").matcher(extractUsername(value) ?: "")
        return value.isBlank() || value.isNotBlank() && matcher.matches()
    }
}


const val BIRTHDAY_DATE_FORMAT = "MM / dd / yyyy"

object DateValidator : TextValidator {
    override fun validate(value: String): Boolean {

        if (value.isEmpty()) {
            return false
        }
        val formatter: DateFormat = SimpleDateFormat(BIRTHDAY_DATE_FORMAT, Locale.US)
        val date = formatter.parse(value) as Date
        val age = Calendar.getInstance()
        age.time = date
        val ageLimit = Calendar.getInstance()
        ageLimit.add(Calendar.YEAR, -AGE_LIMIT)
        return age.before(ageLimit)
    }
}