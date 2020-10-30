package social.tsu.android.network

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import social.tsu.android.R
import java.io.File


class DownloadHelper {
    private var offlineUrl: String = ""
    var onDownloadCompleted: ((url: String) -> Unit)? = null

    fun downloadFile(url: String, mfileName: String, context: Activity) {
        Dexter.withContext(context)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        val fileName = mfileName + url.substring(url.lastIndexOf('.'), url.length)
                        val file = File(Generate.generatedFilepath(context).plus(fileName))
                        offlineUrl = file.absolutePath
                        val request: DownloadManager.Request =
                            DownloadManager.Request(Uri.parse(url))
                                .setTitle(fileName) // Title of the Download Notification
                                .setDescription("Downloading...") // Description of the Download Notification
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                                .setDestinationUri(Uri.fromFile(file)) // Uri of the destination file
                                .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                                .setAllowedOverRoaming(true) // Set if download is allowed on roaming network
                        val downloadManager: DownloadManager =
                            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.enqueue(request) // enqueue puts the download request in the queue.
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).onSameThread().check()

    }

    object Generate {
        private var apkStorage: File? = null
        private var apkStorageOther: File? = null
        private val isSDCardPresent: Boolean =
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

        fun generatedFilepath(context: Context): String {
            if (isSDCardPresent) {
                apkStorage = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.absolutePath
                            + "/" + context.getString(R.string.app_name)
                )
                apkStorageOther = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.absolutePath
                            + "/" + context.getString(R.string.app_name) + "/" + "gif"
                )
            }

            apkStorage?.let {
                if (it.exists().not())
                    it.mkdir()
            }

            apkStorageOther?.let {
                if (it.exists().not())
                    it.mkdir()
            }
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.absolutePath +
                    "/" + context.getString(R.string.app_name).plus("/")
                .plus("gif").plus("/")
        }
    }

    val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id: Long = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val downloadManager: DownloadManager =
                context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val cursor: Cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
            if (cursor.moveToNext()) {
                val status: Int =
                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                cursor.close()
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    onDownloadCompleted?.invoke(offlineUrl)
                } else if (status == DownloadManager.STATUS_FAILED) {
                    onDownloadCompleted?.invoke("")
                    Toast.makeText(
                        context, "Try again.." +
                                "", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun getImageContentUri(
        context: Context,
        imageFile: File
    ): Uri? {
        val filePath = imageFile.absolutePath
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA + "=? ",
            arrayOf(filePath),
            null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            cursor.close()
            Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "" + id
            )
        } else {
            if (imageFile.exists()) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, filePath)
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
            } else {
                null
            }
        }
    }

}




