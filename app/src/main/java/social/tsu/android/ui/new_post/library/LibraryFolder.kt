package social.tsu.android.ui.new_post.library

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LibraryFolder(val folderName: String, val mediaContent: MutableList<LibraryMedia>) :
    Parcelable