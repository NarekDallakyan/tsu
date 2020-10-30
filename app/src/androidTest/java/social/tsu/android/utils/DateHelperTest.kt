package social.tsu.android.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import social.tsu.android.helper.DateHelper
import java.io.InputStreamReader
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*


@RunWith(AndroidJUnit4::class)
class DateHelperTest {

    @Test
    fun testDatePretty() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val sourceFile = javaClass.classLoader?.getResourceAsStream("dates_list.txt")
        assertNotNull("No input file with dates result", sourceFile)

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        var sourceList: List<String>? = null
        InputStreamReader(sourceFile!!).use {
            sourceList = it.readLines()
        }
        assertNotNull("No input date with dates result", sourceFile)

        val dateList = createDatesList()
        sourceList?.forEachIndexed { index, line ->
            val currentDate = dateList[index]
            val currentDateFormat = formatter.format(Date(currentDate))
            val currentPretty = DateHelper.prettyDate(appContext, currentDate)
            assertEquals("Wrong date - $currentDateFormat", currentPretty, line)
        }
    }

    private fun createDatesList(): List<Long> {
        val dateList = arrayListOf<Long>()
        dateList += System.currentTimeMillis()

        for (i in -130..-1) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, i)
            dateList += calendar.timeInMillis
        }

        for (i in -130..-1) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, i)
            dateList += calendar.timeInMillis
        }

        for (i in -50..-1) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR, i)
            dateList += calendar.timeInMillis
        }

        for (i in -720..-1) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, i)
            dateList += calendar.timeInMillis
        }

        for (i in -25..-1) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, i)
            dateList += calendar.timeInMillis
        }

        for (i in -2..-1) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, i)
            dateList += calendar.timeInMillis
        }

        return dateList
    }

}