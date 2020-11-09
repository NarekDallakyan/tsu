package social.tsu.android.ui.post.view.viewpager

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.LifecycleOwner
import social.tsu.cameracapturer.camera.CameraListener
import social.tsu.cameracapturer.camera.CameraView
import social.tsu.cameracapturer.camera.PictureResult
import social.tsu.cameracapturer.filter.Filter
import social.tsu.cameracapturer.helper.CameraCaptureHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoCameraPostHandler(
    val cameraView: CameraView,
    val lifecycleOwner: LifecycleOwner,
    val context: Context
) {

    private var cameraHelper: CameraCaptureHelper? = null

    fun initialize() {
        // Initialize camera helper
        cameraHelper = CameraCaptureHelper()
        cameraHelper?.initialize(
            cameraView, lifecycleOwner, cameraListener
        )
    }

    private var callBack: ((String?) -> Unit) = {}

    fun handleFilter(filters: Filter) {
        cameraHelper?.changeFilter(filters)
    }

    fun switchCamera() {

        cameraHelper?.toggleCamera()
    }

    fun handleFlash() {
        cameraHelper?.handleFlash()
    }

    fun capturePicture(callBack: ((String?) -> Unit) = {}) {
        this.callBack = callBack

        cameraHelper?.capturePicture()
    }

    private val cameraListener = object : CameraListener() {

        override fun onPictureTaken(result: PictureResult) {

            val dateFormat = SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.US)
            val currentTimeStamp = dateFormat.format(Date())

            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + File.separator + "CameraViewFreeDrawing"
            val outputDir = File(path)
            outputDir.mkdirs()
            val saveTo = File(path + File.separator + currentTimeStamp + ".jpg")

            result.toFile(saveTo) {
                // refresh gallery
                MediaScannerConnection.scanFile(
                    context, arrayOf(it.toString()), null
                ) { filePath: String, uri: Uri ->

                    callBack(filePath)
                }
            }
        }
    }
}