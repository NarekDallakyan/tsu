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


class GifPostHandler(
    val cameraView: CameraView,
    val lifecycleOwner: LifecycleOwner,
    val context: Context
) {

    private var cameraHelper: CameraCaptureHelper? = null

    private var onStopCallback: (filePath: String) -> Unit = {}

    fun initialize() {
        // Initialize camera helper
        cameraHelper = CameraCaptureHelper()
        cameraHelper?.initialize(
            cameraView, lifecycleOwner, cameraListener
        )
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

    fun handleFilter(filters: Filter) {
        cameraHelper?.changeFilter(filters)
    }

    fun stopRecordGif(onStopCallback: (filePath: String) -> Unit = {}) {
        this.onStopCallback = onStopCallback
        cameraView.stopVideo()
    }

    fun recordGif() {

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