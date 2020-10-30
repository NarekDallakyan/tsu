package social.tsu.android.ui.util

import android.os.Bundle
import android.os.Parcel

class BundleUtils {

    companion object {
        fun getBundleSizeInBytes(bundle: Bundle?): Int {
            val parcel = Parcel.obtain()
            val size: Int
            parcel.writeBundle(bundle)
            size = parcel.dataSize()
            parcel.recycle()
            return size
        }
    }
}