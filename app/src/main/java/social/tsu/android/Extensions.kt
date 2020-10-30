package social.tsu.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.Executors

fun Double.currency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.maximumFractionDigits = 4
    formatter.minimumFractionDigits = 4
    return formatter.format(this)
}


fun execute(block: () -> Unit) = Executors.newSingleThreadExecutor().execute(block)

fun <T> LiveData<T>.observeOnce(block: (t: T) -> Unit) {

    observeForever(object : Observer<T> {
        override fun onChanged(t: T?) {
            if (t != null) {
                block.invoke(t)
                removeObserver(this)
            }
        }
    })
}

fun LocalDate.getWeekTimelineLabel(): String {
    return "${this.minusWeeks(1).formatDateToEEEdd()} - ${this.formatDateToEEEdd()}"
}

fun LocalDate.getMonthTimelineLabel(minusMonth: Long): String {
    return "${this.minusMonths(minusMonth).formatDateToMMdd()} - ${this.formatDateToMMdd()}"
}

fun LocalDate.formatDateToEEEdd(): String {
    val formatter = DateTimeFormatter.ofPattern("EEE dd")
    return this.format(formatter)
}

fun LocalDate.formatDateToMMdd(): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    return this.format(formatter)
}

fun LocalDate.formatDateToQueryArgument(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return this.format(formatter)
}

