package social.tsu.android.ui.new_post.library

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class LibraryMedia(var uri: Uri, var mimeType: String) :Parcelable{

    fun isImage()= mimeType.contains("image")

    fun isVideo() = mimeType.contains("video")

}