package social.tsu.android.ui.util

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Path
import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class FlyEmojiAnimation(private val context: Context, private val container: ViewGroup) {

    fun flyEmoji(emojiText: String) {

        val duration: Long = 3000
        val view = TextView(context).apply {
            text = emojiText
            textSize = 25f
        }

        val points = generatePoints(container, view)

        val path = generatePath(points, calculateConnectionPointsForBezierCurve(points))

        container.addView(view)
        val animator = ObjectAnimator.ofFloat(view, "x", "y", path)
            .setDuration(duration)
        val opacityAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
            .setDuration(duration)
        val listener = object : Animator.AnimatorListener {
            override fun onAnimationRepeat(p0: Animator?) {

            }

            override fun onAnimationEnd(p0: Animator?) {
                container.removeView(view)
            }

            override fun onAnimationCancel(p0: Animator?) {
                container.removeView(view)
            }

            override fun onAnimationStart(p0: Animator?) {

            }
        }
        animator.addListener(listener)
        animator.start()
        opacityAnimator.start()


    }

    private fun generatePoints(container: View, viewToFly: TextView): ArrayList<PointF> {
        val size = 3
        val baseY = container.measuredHeight - viewToFly.textSize
        val baseXStart = 0
        val baseXEnd = container.measuredWidth - viewToFly.textSize.toInt()

        val yDiff = container.measuredHeight.toFloat() / size

        val points = arrayListOf<PointF>()
        for (i in 0 until size) {
            val pointF = PointF((baseXStart until baseXEnd).random().toFloat(), baseY - (i * yDiff))
            points.add(pointF)
        }
        return points
    }

    private fun calculateConnectionPointsForBezierCurve(points: ArrayList<PointF>): Pair<ArrayList<PointF>, ArrayList<PointF>> {
        val conPoint1 = arrayListOf<PointF>()
        val conPoint2 = arrayListOf<PointF>()
        for (i in 1 until points.size) {
            conPoint1.add(PointF(points[i - 1].x, (points[i].y + points[i - 1].y) / 2))
            conPoint2.add(PointF(points[i].x, (points[i].y + points[i - 1].y) / 2))
        }
        return conPoint1 to conPoint2
    }

    private fun generatePath(
        points: ArrayList<PointF>,
        connectionPoints: Pair<ArrayList<PointF>, ArrayList<PointF>>
    ): Path {

        val path = Path()
        val conPoint1 = connectionPoints.first
        val conPoint2 = connectionPoints.second
        if (points.isEmpty() && conPoint1.isEmpty() && conPoint2.isEmpty()) return path

        path.reset()
        path.moveTo(points.first().x, points.first().y)

        for (i in 1 until points.size) {
            path.cubicTo(
                conPoint1[i - 1].x, conPoint1[i - 1].y, conPoint2[i - 1].x, conPoint2[i - 1].y,
                points[i].x, points[i].y
            )
        }
        return path
    }
}