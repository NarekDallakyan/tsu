package social.tsu.android.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.AspectRatio
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


object CameraUtil {

    const val MODE_LIBRARY = 100
    const val MODE_CAMERA  = 200
    const val EXTRA_CUSTOM_CAMERA_MODE = "custom_camera_mode"
    const val EXTRA_IMAGE_PATH = "imagePath"

    const val RATIO_4_3_VALUE = 4.0 / 3.0
    const val RATIO_16_9_VALUE = 16.0 / 9.0

    val EXTENSION_WHITELIST = arrayOf("JPG")

    fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)

        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /**
     * Creates file in the provided base dir and with given filename pattern and extension
     */
    fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )

    fun getBitmapFromContent(context: Context, photoUri: Uri): File? {
        photoUri.authority?.let {
            try {
                context.contentResolver.openInputStream(photoUri).use {
                    return createTemporalFileFrom(context, it)
                }
            } catch (e: IOException) {
                Log.e("CameraUtil", "getBitmapFromContent", e)
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun createTemporalFileFrom(context: Context, inputStream: InputStream?): File? {
        if (inputStream == null) return null
        var read: Int
        val buffer = ByteArray(8 * 1024)
        val targetFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(targetFile).use { out ->
            while (inputStream.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            out.flush()
        }
        return targetFile
    }

}
