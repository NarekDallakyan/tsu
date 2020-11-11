package social.tsu.android.ext

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import pl.droidsonroids.gif.GifDrawable
import social.tsu.android.TsuApplication
import java.io.File

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

fun String.getGifDrawable(): GifDrawable {

    val uri = Uri.fromFile(File(this))
    val contentProvider = TsuApplication.mContext.contentResolver
    return GifDrawable(contentProvider, uri)
}