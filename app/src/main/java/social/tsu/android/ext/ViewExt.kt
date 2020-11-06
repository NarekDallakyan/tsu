package social.tsu.android.ext

import android.app.Activity
import android.content.Context
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
    inputMethodManager.toggleSoftInputFromWindow(
        this.applicationWindowToken,
        InputMethodManager.SHOW_FORCED, 0
    )
}