package social.tsu.android.ext

import android.view.View
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