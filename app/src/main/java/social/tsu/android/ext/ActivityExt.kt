package social.tsu.android.ext

import android.app.Activity
import android.graphics.Rect
import android.view.ViewTreeObserver
import social.tsu.android.utils.KeyboardListener

fun Activity.addOnKeyboardListener(keyboardListener: KeyboardListener) {

    // Threshold for minimal keyboard height.
    val minKeyboardHeightPx = 150
    val decorView = this.window.decorView
    // Register global layout listener.
    decorView.viewTreeObserver?.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        private val windowVisibleDisplayFrame: Rect = Rect()
        private var lastVisibleDecorViewHeight = 0
        override fun onGlobalLayout() {
            // Retrieve visible rectangle inside window.
            decorView.getWindowVisibleDisplayFrame(windowVisibleDisplayFrame)
            val visibleDecorViewHeight: Int = windowVisibleDisplayFrame.height()
            // Decide whether keyboard is visible from changing decor view height.
            if (lastVisibleDecorViewHeight != 0) {
                if (lastVisibleDecorViewHeight > visibleDecorViewHeight + minKeyboardHeightPx) {
                    // Calculate current keyboard height (this includes also navigation bar height when in fullscreen mode).
                    val currentKeyboardHeight: Int =
                        decorView.height.minus(windowVisibleDisplayFrame.bottom)
                    // Notify listener about keyboard being shown.
                    keyboardListener.onKeyboardShown()
                } else if (lastVisibleDecorViewHeight + minKeyboardHeightPx < visibleDecorViewHeight) {
                    // Notify listener about keyboard being hidden.
                    keyboardListener.onKeyboardHidden()
                }
            }
            // Save current decor view height for the next call.
            lastVisibleDecorViewHeight = visibleDecorViewHeight
        }
    })
}