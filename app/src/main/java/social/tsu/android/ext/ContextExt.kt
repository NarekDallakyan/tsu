package social.tsu.android.ext

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import social.tsu.android.TsuApplication
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

@Suppress("SameParameterValue")
@SuppressLint("SimpleDateFormat")
private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
    SimpleDateFormat("dd.MM.yyyy").let { formatter ->
        TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
    }

fun Context.getLastPicture(): Uri? {

    val mContext = TsuApplication.mContext
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED
    )
    val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

    val selectionArgs = arrayOf(
        // Release day of the G1. :)
        dateToTimestamp(day = 22, month = 10, year = 2008).toString()
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    mContext.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            cursor.close()
            return ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
        }
    }
    return null
}