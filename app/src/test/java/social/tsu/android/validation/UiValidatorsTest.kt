package social.tsu.android.validation

import org.junit.Assert.*
import org.junit.Test
import social.tsu.android.AGE_LIMIT
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class UiValidatorsTest {

    @Test
    fun testPasswordValidator() {
        val correctPassword = "Pas\$w0rd25"
        val shortPassword = "Pas\$w0r"
        val noSymbolPassword = "Passw0rd25"
        val noDigitPassword = "Passwordnd"
        val noCapsPassword = "pas\$w0rd25"
        assertTrue(PasswordValidator.validate(correctPassword))
        assertFalse(PasswordValidator.validate(shortPassword))
        assertFalse(PasswordValidator.validate(noSymbolPassword))
        assertFalse(PasswordValidator.validate(noDigitPassword))
        assertFalse(PasswordValidator.validate(noCapsPassword))
    }

    @Test
    fun testEmailValidator() {
        val correctEmail = "testuser.dev@gmail.com"
        val invalidEmail = "noat.com"
        val specCharEmail = "testuser%@gmail.com"
        val noDomainEmail = "testuser@gmail"
        assertTrue(EmailValidator.validate(correctEmail))
        assertFalse(EmailValidator.validate(invalidEmail))
        assertFalse(EmailValidator.validate(specCharEmail))
        assertFalse(EmailValidator.validate(noDomainEmail))
    }

    @Test
    fun testNonBlankValidator() {
        val correctValue = "SomeValue"
        val blankValue = ""
        assertTrue(TextNotEmptyValidator.validate(correctValue))
        assertFalse(TextNotEmptyValidator.validate(blankValue))
    }

    @Test
    fun testDateValidator() {
        val formatter: DateFormat = SimpleDateFormat(BIRTHDAY_DATE_FORMAT, Locale.US)
        val correctAge = Calendar.getInstance()
        correctAge.add(Calendar.YEAR, -(AGE_LIMIT + 3)) //3 years more than age limit
        val incorrectAge = Calendar.getInstance()
        incorrectAge.add(Calendar.YEAR, -(AGE_LIMIT -3)) //3 years more than age limit

        val correctValue = "SomeValue"
        val blankValue = ""
        assertTrue(DateValidator.validate(formatter.format(correctAge.time)))
        assertFalse(DateValidator.validate(formatter.format(incorrectAge.time)))
    }
}