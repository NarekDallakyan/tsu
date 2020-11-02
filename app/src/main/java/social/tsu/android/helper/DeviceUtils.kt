package social.tsu.android.helper

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity


class DeviceUtils {
    //TODO: Needs proper id generation.
    companion object {
        fun getDeviceId(): String {
            return "someId"
        }

        fun pixelsToSp(context: Context, px: Float): Float {
            val scaledDensity = context.resources.displayMetrics.scaledDensity
            return px / scaledDensity
        }

        fun getDeviceFullHeight(context: Context): Float {

            val displayMetrics = DisplayMetrics()
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            var height = displayMetrics.heightPixels.toFloat()

            val resources = context.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                height += resources.getDimensionPixelSize(resourceId).toFloat()
            }

            return height
        }

        fun getDeviceWidth(activity: FragmentActivity): Float {

            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels.toFloat()
        }

    }
}
