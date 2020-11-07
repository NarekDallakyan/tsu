package social.tsu.android.ext

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import social.tsu.android.TsuApplication

fun String.getVideoThumbnail(): Bitmap? {

    var mediaMetadataRetriever: MediaMetadataRetriever? = null
    return try {
        mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(TsuApplication.mContext, Uri.parse(this))
        mediaMetadataRetriever.frameAtTime
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        mediaMetadataRetriever?.release()
    }
}