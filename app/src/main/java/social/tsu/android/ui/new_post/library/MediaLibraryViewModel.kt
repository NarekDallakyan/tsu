package social.tsu.android.ui.new_post.library

import androidx.lifecycle.ViewModel
import social.tsu.android.data.repository.MediaLibraryRepository
import javax.inject.Inject

class MediaLibraryViewModel @Inject constructor(
    private val mediaLibraryRepo: MediaLibraryRepository
) : ViewModel() {

    var allowVideo
        get() = mediaLibraryRepo.allowVideo
        set(value) {
            mediaLibraryRepo.allowVideo = value
        }

    val mediaLibraryFolders by lazy {
        mediaLibraryRepo.queryMediaFolders()
        mediaLibraryRepo.mediaFolders
    }

}