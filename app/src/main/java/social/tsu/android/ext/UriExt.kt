package social.tsu.android.ext

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

fun Uri.getRealPathFromURI(context: Context): String? {

    var cursor: Cursor? = null
    return try {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        cursor = this.let { context.contentResolver.query(it, proj, null, null, null) }
        val column_index: Int? = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        cursor?.getString(column_index!!)
    } finally {
        cursor?.close()
    }
}