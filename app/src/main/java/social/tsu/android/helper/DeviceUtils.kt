package social.tsu.android.helper

import android.content.Context


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
    }
}
