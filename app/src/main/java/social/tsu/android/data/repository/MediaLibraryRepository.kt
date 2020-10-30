package social.tsu.android.data.repository

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import social.tsu.android.R
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.new_post.library.LibraryFolder
import social.tsu.android.ui.new_post.library.LibraryMedia
import java.util.concurrent.Executors
import javax.inject.Inject


class MediaLibraryRepository @Inject constructor(
    private val application: Application
) {

    var allowVideo: Boolean = true

    private val _mediaFoldersLoadState = MutableLiveData<Data<List<LibraryFolder>>>()
    val mediaFolders:LiveData<Data<List<LibraryFolder>>> = _mediaFoldersLoadState

    private var _cursor: Cursor? = null

    fun queryMediaFolders() = execute {
        // Get relevant columns
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.TITLE
        )

        // Return only video and image metadata.
        var selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)

        if (allowVideo) {
            selection += (" OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        }

        val queryUri = MediaStore.Files.getContentUri("external")

        _cursor = application.applicationContext
            .contentResolver.query(queryUri,
                projection,
                selection,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
            )
        _cursor?.let {
            _mediaFoldersLoadState.postValue(Data.Success(extractLibraryFolders(it)))
            _cursor?.close()
        }

    }

    private fun extractLibraryFolders(cursor: Cursor):List<LibraryFolder>{
        val recentsFolderName = application.getString(R.string.recents)
        val libraryFolders = arrayListOf(LibraryFolder(recentsFolderName, arrayListOf()))

        val columnIndexFolderName =
            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val mediaFolderName = cursor.getString(columnIndexFolderName)?: recentsFolderName

            val mediaFolderIndex = libraryFolders.indexOfFirst { it.folderName ==  mediaFolderName}

            val mediaItem = extractMediaItem(cursor)?:continue
            if(mediaFolderIndex<0){
                libraryFolders.add(LibraryFolder(mediaFolderName, arrayListOf(mediaItem)))
            }else{
                libraryFolders[mediaFolderIndex].mediaContent.add(mediaItem)
            }

            // add media to recents by default
            if(mediaFolderName != recentsFolderName) libraryFolders[0].mediaContent.add(mediaItem)

        }

        cursor.close()

        return if(libraryFolders.size == 1 && libraryFolders[0].mediaContent.isEmpty()) arrayListOf() else libraryFolders
    }

    private fun extractMediaItem(cursor: Cursor): LibraryMedia?{
        val columnIndexId = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val columnIndexMimeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

        val mimeType = cursor.getString(columnIndexMimeType)?: return null
        val uriExternal = if(mimeType.contains("image")){
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val mediaId = cursor.getString(columnIndexId)

        val mediaUri = Uri.withAppendedPath(uriExternal, "" + mediaId)

        return LibraryMedia(
            mediaUri,
            mimeType
        )
    }

    private fun execute(block:()->Unit) = Executors.newSingleThreadExecutor().execute(block)

}