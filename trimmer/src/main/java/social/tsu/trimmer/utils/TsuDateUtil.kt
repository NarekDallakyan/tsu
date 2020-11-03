package social.tsu.trimmer.utils

import java.util.concurrent.TimeUnit

object TsuDateUtil {

    fun calculateTime(seconds: Long): String? {
        val day = TimeUnit.SECONDS.toDays(seconds).toInt()
        val hours = TimeUnit.SECONDS.toHours(seconds) - day * 24
        val minute = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.SECONDS.toHours(seconds) * 60
        val second = TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.SECONDS.toMinutes(seconds) * 60
        var hoursText = hours.toString()
        var minuteText = minute.toString()
        var secondText = second.toString()

        // Case 1
        if (hoursText.length == 1) {
            hoursText = "0$hoursText"
        }
        if (minuteText.length == 1) {
            minuteText = "0$minuteText"
        }
        if (secondText.length == 1) {
            secondText = "0$secondText"
        }

        // Case 2
        if (hoursText.length == 0) {
            hoursText = "00"
        }
        if (minuteText.length == 0) {
            minuteText = "00"
        }
        if (secondText.length == 0) {
            secondText = "00"
        }
        return "$hoursText:$minuteText:$secondText"
    }
}