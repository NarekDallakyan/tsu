package social.tsu.android.ui.post.view.viewpager

import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.VideoCaptureConfig
import social.tsu.android.ui.CameraUtil
import social.tsu.android.ui.new_post.BaseCameraFragment
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.ui.post.view.RecordingState
import social.tsu.android.utils.hide
import social.tsu.android.utils.pickMediaFromGallery
import java.io.File


class GifPostFragment : BaseCameraFragment<VideoCapture>() {

    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val VIDEO_EXTENSION = ".mp4"
    }

    private var recordingState: RecordingState = RecordingState.Stopped

    private var callBack: ((onCancel: Boolean, onStart: Boolean) -> Unit)? =
        null

    override fun onPause() {
        if (recordingState is RecordingState.Recording) {
            recordingState = RecordingState.Canceled
            capture?.stopRecording()
            onStopRecording()

            try {
                Thread.sleep(500)
                super.onPause()
            } catch (e: Exception) {
                Log.e(TAG, "stopRecording onPause", e)
            }
        } else {
            super.onPause()
        }
    }

    override fun createCapture(aspectRatio: Int, rotation: Int): VideoCapture {

        return VideoCaptureConfig.Builder()
            .setTargetRotation(rotation)
            .setTargetAspectRatio(aspectRatio)
            .build()
    }

    override fun onCaptureImageClick(button: ImageButton) {

        when (recordingState) {
            is RecordingState.Stopped,
            is RecordingState.Canceled -> {
                val videoFile = CameraUtil.createFile(
                    outputDirectory,
                    FILENAME,
                    VIDEO_EXTENSION
                )
                recordingState = RecordingState.Recording
                button.isActivated = true
                cameraSwitchButton.hide()
                capture?.startRecording(
                    videoFile,
                    cameraExecutor,
                    object : VideoCapture.OnVideoSavedCallback {
                        override fun onVideoSaved(file: File) {
                            if (recordingState != RecordingState.Canceled) {
                                //Pass recorded file to PostTypesFragment
                                val fragment = parentFragment
                                if (fragment is PostTypesFragment) {
                                    this@GifPostFragment.callBack?.let {
                                        it(false, false)
                                    }
                                    Log.d(TAG, "Saved video $file")
                                    fragment.next(videoPath = file.toString())
                                    return
                                }
                            }

                            Log.d(TAG, "Recording video canceled.")
                            this@GifPostFragment.callBack?.let {
                                it(true, false)
                            }
                        }

                        override fun onError(
                            videoCaptureError: Int,
                            message: String,
                            cause: Throwable?
                        ) {
                            onStopRecording()
                            recordingState = RecordingState.Stopped
                            if (cause != null) {
                                cause.printStackTrace()
                                Log.e(TAG, "Error on save video", cause)
                            }

                            this@GifPostFragment.callBack?.let {
                                it(true, false)
                            }
                        }

                    })
            }
            is RecordingState.Recording -> {
                recordingState = RecordingState.Stopped
                capture?.stopRecording()
                onStopRecording()
            }
        }
    }

    override fun onDisplayChanged(view: View) {
        capture?.setTargetRotation(view.display.rotation)
    }

    override fun onGetPickResult(data: Intent?) {
        val videoPath = data?.data ?: return

        Log.d("videoCapture", "video path = $videoPath")
        val contentUri = MediaStore.Video.Media.getContentUri(videoPath.toString())
        Log.d("videoCapture", "content path = $contentUri")

        val fragment = parentFragment?.parentFragment
        if (fragment is PostTypesFragment) {
            fragment.next(videoContentUri = videoPath.toString())
        }
    }

    override fun pickFromGallery() {
        pickMediaFromGallery(REQUEST_PICK_LIBRARY, "video/*")
    }

    override fun canSwitchCamera(): Boolean {
        //Disable switching cameras while recording
        return recordingState != RecordingState.Recording
    }

    private fun onStopRecording() {

    }

    fun stopRecording() {

        startCapturing {

            if (it) {
                this.callBack?.let {
                    it(true, false)
                }
            }
        }
    }

    fun recordGif(callBack: (onCancel: Boolean, onStart: Boolean) -> Unit) {

        this.callBack = callBack
        startCapturing {

            if (it) {
                this.callBack?.let {
                    it(true, false)
                }
            }
        }
        this.callBack?.let {
            it(false, true)
        }
    }
}