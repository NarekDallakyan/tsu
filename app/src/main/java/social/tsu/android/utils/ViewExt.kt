package social.tsu.android.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.view.children
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import social.tsu.android.R


fun BottomNavigationView.getMenuItemView(index: Int): BottomNavigationItemView? {
    val itemParent = getChildAt(0) as ViewGroup
    if (index < itemParent.childCount) {
        val item = itemParent.getChildAt(index)
        return item as? BottomNavigationItemView
    }
    return null
}

fun BottomNavigationView.getMenuItemViewById(id: Int): BottomNavigationItemView? {
    val itemParent = getChildAt(0) as ViewGroup

    return itemParent.children.first { it.id == id } as? BottomNavigationItemView
}

@SuppressLint("RestrictedApi")
fun BottomNavigationItemView.setIconSizeDimen(@DimenRes sizeRes: Int) {
    setIconSize(resources.getDimensionPixelSize(sizeRes))
}

fun ViewGroup.replaceChildWithView(childIdx: Int, view: View) {
    if (childIdx < childCount) {
        val layoutParams = getChildAt(childIdx).layoutParams
        removeViewAt(childIdx)
        view.layoutParams = layoutParams
        addView(view, childIdx)
    }
}

fun View?.show() {
    if (this == null) return
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
    }
}

fun View?.hide() {
    if (this == null) return
    if (visibility != View.GONE) {
        visibility = View.GONE
    }
}

fun View.OnClickListener.applyTo(vararg views: View?) {
    views.forEach {
        it?.setOnClickListener(this)
    }
}

fun TextInputLayout.setErrorResource(@StringRes messageRes: Int) {
    error = context.getString(messageRes)
}

fun TabLayout.Tab.customizeTitle(customize: (TextView) -> Unit) {
    val textView = view.findViewById<TextView>(android.R.id.text1)
    if (textView is TextView) {
        customize(textView)
        textView.measure(View.MeasureSpec.UNSPECIFIED, textView.height)
        textView.measure(View.MeasureSpec.UNSPECIFIED, textView.width)
    }
}

fun View.getSnackBar(message: String): Snackbar {
    val sb = Snackbar.make(
        this, message,
        Snackbar.LENGTH_LONG
    ).setBackgroundTint(context.getColor(R.color.tsu_green))
        .setTextColor(Color.BLACK)

    val view: View = sb.view
    val tv =
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)

    tv.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        resources.getDimension(R.dimen.font_medium)
    )
    tv.maxLines = 4
    return sb
}

fun View.setOnKeyboardOpenListener(onOpen: () -> Unit, onClose: () -> Unit = {}) {
    var isKeyboardShowing = false
    viewTreeObserver.addOnGlobalLayoutListener {
        val r = Rect()
        getWindowVisibleDisplayFrame(r)
        val screenHeight = rootView.height

        // r.bottom is the position above soft keypad or device button.
        // if keypad is shown, the r.bottom is smaller than that before.
        val keypadHeight = screenHeight - r.bottom

        if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
            // keyboard is opened
            if (!isKeyboardShowing) {
                isKeyboardShowing = true
                onOpen()
            }
        } else {
            // keyboard is closed
            if (isKeyboardShowing) {
                isKeyboardShowing = false
                onClose()
            }
        }
    }
}