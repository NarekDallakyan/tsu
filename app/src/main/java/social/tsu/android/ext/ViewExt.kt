package social.tsu.android.ext

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import social.tsu.android.ui.util.LitAnimationHelper


fun View.show(animate: Boolean = false, duration: Long = 300) {

    if (this.visibility == View.VISIBLE) return

    if (!animate) {

        visibility = View.VISIBLE
        return
    }

    LitAnimationHelper()
        .showWithAlpha(this, duration)
}

fun View.isShow(): Boolean {
    return visibility == View.VISIBLE
}

fun View.hide(invisible: Boolean = false, animate: Boolean = false, duration: Long = 200) {

    if (this.visibility != View.VISIBLE) return

    if (!animate) {

        visibility = if (invisible) {
            View.INVISIBLE
        } else {
            View.GONE
        }
        return
    }

    LitAnimationHelper()
        .hideWithAlpha(this, duration)
}

fun View.hideKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.showKeyboard(activity: Activity) {

    val inputMethodManager =
    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

@SuppressLint("ClickableViewAccessibility")
fun View.onSwipeListener(callback: (left: Boolean) -> Unit) {

    var x1: Float = 0f
    var x2: Float = 0f

    this.setOnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                x1 = event.x
            }
            MotionEvent.ACTION_UP -> {
                x2 = event.x
                val deltaX = x2 - x1
                if (deltaX > 0) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        }
        return@setOnTouchListener onTouchEvent(event)
    }
}
