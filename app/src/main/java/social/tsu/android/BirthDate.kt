package social.tsu.android

import java.util.*

const val AGE_LIMIT = 17

@SuppressWarnings("MagicNumber")
data class BirthDate(val month: Int, val day: Int, val year: Int) {
    val monthString
    get() = month.toString()

    val dayString
    get() = day.toString()

    val yearString
    get() = year.toString()

    val stringValue
    get() = "$month / $day / $year"


    fun isAllowedAge(): Boolean {
        val ageLimit = Calendar.getInstance()
        ageLimit.add(Calendar.YEAR, -AGE_LIMIT)
        val age = GregorianCalendar(year, month -1 , day)
        return age.before(ageLimit)
    }
}
