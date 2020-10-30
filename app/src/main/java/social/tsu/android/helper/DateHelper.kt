package social.tsu.android.helper

import android.content.Context
import android.text.format.DateUtils
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import social.tsu.android.R
import java.util.*

object DateHelper {

    fun prettyDate(context: Context, createdAt: Date): CharSequence? {
        return prettyDate(context, createdAt.time)
    }

    fun prettyDate(context: Context, createdAt: Long): CharSequence? {
        val now = System.currentTimeMillis()
        val differ = now - createdAt

        if (differ < DateUtils.SECOND_IN_MILLIS) {
            return context.getString(R.string.date_moment_ago)
        }

        if (differ < DateUtils.MINUTE_IN_MILLIS) {
            val result = differ / DateUtils.SECOND_IN_MILLIS
            return if (result > 1) {
                context.getString(R.string.date_format_seconds_ago, result)
            } else {
                context.getString(R.string.date_one_second_ago)
            }
        }

        if (differ < DateUtils.HOUR_IN_MILLIS) {
            val result = differ / DateUtils.MINUTE_IN_MILLIS
            return if (result > 1) {
                context.getString(R.string.date_format_minutes_ago, result)
            } else {
                context.getString(R.string.date_one_minute_ago)
            }
        }

        if (differ < DateUtils.DAY_IN_MILLIS) {
            val result = differ / DateUtils.HOUR_IN_MILLIS
            return if (result > 1) {
                context.getString(R.string.date_format_hours_ago, result)
            } else {
                context.getString(R.string.date_one_hour_ago)
            }
        }

        val createdAtCalendar = Calendar.getInstance().apply { timeInMillis = createdAt }
        val days = (differ / DateUtils.DAY_IN_MILLIS).toInt()
        val maxDaysOfMonth = createdAtCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (days < maxDaysOfMonth) {
            val result = differ / DateUtils.DAY_IN_MILLIS
            return if (result > 1) {
                context.getString(R.string.date_format_days_ago, result)
            } else {
                context.getString(R.string.date_one_day_ago)
            }
        }

        val calendar = Calendar.getInstance()

        val minuteDiffer = calendar.minusField(createdAtCalendar, Calendar.MINUTE)
        val hourDiffer = calendar.minusField(createdAtCalendar, Calendar.HOUR_OF_DAY) - if (minuteDiffer < 0) 1 else 0
        val dayDiffer = calendar.minusField(createdAtCalendar, Calendar.DAY_OF_MONTH) - if (hourDiffer < 0) 1 else 0
        var monthDiffer = calendar.minusField(createdAtCalendar, Calendar.MONTH) - if (dayDiffer < 0) 1 else 0
        val yearDiffer = calendar.minusField(createdAtCalendar, Calendar.YEAR) - if (monthDiffer < 0) 1 else 0

        if (yearDiffer > 1) {
            return context.getString(R.string.date_format_years_ago, yearDiffer)
        } else if (yearDiffer == 1) {
            return context.getString(R.string.date_one_year_ago)
        }

        if (monthDiffer < 0) {
            monthDiffer += 12
        }

        if (monthDiffer > 1) {
            return context.getString(R.string.date_format_months_ago, monthDiffer)
        } else if (monthDiffer == 1) {
            return context.getString(R.string.date_one_month_ago)
        }

        return null
    }

    fun getCurrentDate() = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

private fun Calendar.minusField(calendar: Calendar, field: Int): Int {
    return this.get(field) - calendar.get(field)
}


