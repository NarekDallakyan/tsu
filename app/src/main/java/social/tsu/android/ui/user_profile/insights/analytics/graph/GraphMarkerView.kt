package social.tsu.android.ui.user_profile.insights.analytics.graph

import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import social.tsu.android.R

class GraphMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val counterValueTextView = findViewById<TextView>(R.id.counterValue)

    override fun refreshContent(entry: Entry?, highlight: Highlight?) {
        val value = entry?.y?.toDouble() ?: 0.0
        counterValueTextView.text = "${value.toInt()}"
        super.refreshContent(entry, highlight)
    }

    override fun draw(canvas: Canvas, posX: Float, posY: Float) {
        val saveId = canvas.save()
        // translate to the correct position and draw
        canvas.translate(posX + (-width / 2f), 0f)
        draw(canvas)
        canvas.restoreToCount(saveId)
    }
}
