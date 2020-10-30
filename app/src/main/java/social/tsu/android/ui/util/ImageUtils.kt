package social.tsu.android.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.net.toFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

class ImageUtils {

    companion object {

        fun imageDataFrom(uri: Uri?): String? {
            val path = uri?: return null
            val fin = FileInputStream(path.toFile())
            val sourceBitmap = BitmapFactory.decodeStream(fin)
            val matrix = Matrix()
            val bitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val byteArrayImage = baos.toByteArray()
            //TODO: fix code below as its creating video issues
            //cleanup bitmaps
           // sourceBitmap.recycle()
           // bitmap.recycle()
            return "data:image/png;base64," + Base64.encodeToString(byteArrayImage, Base64.DEFAULT)
        }
    }
}