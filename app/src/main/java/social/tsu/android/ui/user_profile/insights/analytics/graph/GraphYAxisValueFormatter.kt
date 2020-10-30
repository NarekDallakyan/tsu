package social.tsu.android.ui.user_profile.insights.analytics.graph

import com.github.mikephil.charting.formatter.ValueFormatter

class GraphYAxisValueFormatter: ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return "${value.toInt()}"
    }
}