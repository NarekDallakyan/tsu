package social.tsu.android.ui.user_profile.insights.analytics.graph

import com.github.mikephil.charting.formatter.ValueFormatter
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class GraphXAxisValueFormatter : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        val localDateFromFloat = LocalDate.ofEpochDay(value.toLong())
        val formatter = DateTimeFormatter.ofPattern("MM/dd")
        return localDateFromFloat.format(formatter)
    }

}