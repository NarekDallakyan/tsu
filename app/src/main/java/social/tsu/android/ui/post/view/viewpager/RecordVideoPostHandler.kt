package social.tsu.android.ui.post.view.viewpager

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import social.tsu.cameracapturer.camera.CameraListener
import social.tsu.cameracapturer.camera.CameraView
import social.tsu.cameracapturer.camera.VideoResult
import social.tsu.cameracapturer.filter.Filter
import social.tsu.cameracapturer.helper.CameraCaptureHelper

class RecordVideoPostHandler(
    val cameraView: CameraView,
    val lifecycleOwner: LifecycleOwner,
    val context: Context
) {

    private var onStopCallback: (filePath: String) -> Unit = {}
    private var cameraHelper: CameraCaptureHelper? = null

    fun initialize() {
        // Initialize camera helper
        cameraHelper = CameraCaptureHelper()
        cameraHelper?.initialize(
            cameraView, lifecycleOwner, cameraListener
        )
    }

    fun handleFilter(filters: Filter) {
        cameraHelper?.changeFilter(filters)
    }

    fun switchCamera() {

        cameraHelper?.toggleCamera()
    }

    fun handleFlash() {
        cameraHelper?.handleFlash()
    }

    fun isRecording(): Boolean {

        return cameraHelper?.isRecording ?: false
    }

    fun stopRecording(onStopCallback: (filePath: String) -> Unit = {}) {
        this.onStopCallback = onStopCallback
        cameraView.stopVideo()
    }

    fun recordVideo() {

        cameraHelper?.startRecording()
    }

    private val cameraListener = object : CameraListener() {

        override fun onVideoTaken(result: VideoResult) {

            // refresh gallery
            MediaScannerConnection.scanFile(
                context, arrayOf(result.file.toString()), null
            ) { filePath: String, uri: Uri ->
                onStopCallback(filePath)
                Log.i("ExternalStorage", "Scanned $filePath:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }
        }
    }
}