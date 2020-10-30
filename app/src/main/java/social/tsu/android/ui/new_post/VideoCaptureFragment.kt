package social.tsu.android.ui.new_post

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.VideoCaptureConfig
import social.tsu.android.ui.CameraUtil
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.hide
import social.tsu.android.utils.pickMediaFromGallery
import social.tsu.android.utils.show
import java.io.File

/**
 * Filename pattern for saved file
 */
private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
private const val VIDEO_EXTENSION = ".mp4"

/**
 * Fragment responsible for video capture and video selection from gallery.
 *
 * @see BaseCameraFragment
 */
class VideoCaptureFragment : BaseCameraFragment<VideoCapture>() {

    private var recordingState: RecordingState = RecordingState.Stopped

    @SuppressLint("RestrictedApi")
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

    @SuppressLint("RestrictedApi")
    override fun createCapture(aspectRatio: Int, rotation: Int): VideoCapture {
        return VideoCaptureConfig.Builder()
            .setTargetRotation(rotation)
            .setTargetAspectRatio(aspectRatio)
            .build()
    }

    //Record button callback. Button states handling and video saving happens here
    @SuppressLint("RestrictedApi")
    override fun onCaptureImageClick(button: ImageButton) {
        Log.d(TAG, "state $recordingState")
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
                                val fragment = parentFragment?.parentFragment
                                if (fragment is PostTypesFragment) {
                                    Log.d(TAG, "Saved video $file")
                                    fragment.next(videoPath = file.toString())
                                    return
                                }
                            }

                            Log.d(TAG, "Recording video canceled.")
                        }

                        override fun onError(
                            videoCaptureError: Int,
                            message: String,
                            cause: Throwable?
                        ) {
                            onStopRecording()
                            recordingState = RecordingState.Stopped
                            if (cause != null) {
                                cause.printStackTrace();
                                Log.e(TAG, "Error on save video", cause)
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

    override fun pickFromGallery() {
        pickMediaFromGallery(REQUEST_PICK_LIBRARY, "video/*")
    }

    /**
     * handle intent from chooser and get picked file
     * After that .next() on parent fragment gets called that launches post creation interface with
     * preview of selected video
     */
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

    @SuppressLint("RestrictedApi")
    override fun onDisplayChanged(view: View) {
        capture?.setTargetRotation(view.display.rotation)
    }

    override fun canSwitchCamera(): Boolean {
        //Disable switching cameras while recording
        return recordingState != RecordingState.Recording
    }

    private fun onStopRecording() {
        cameraCaptureButton.isActivated = false
        cameraSwitchButton.show()
    }

}


sealed class RecordingState {
    object Recording : RecordingState()
    object Stopped : RecordingState()
    object Canceled : RecordingState()
}
