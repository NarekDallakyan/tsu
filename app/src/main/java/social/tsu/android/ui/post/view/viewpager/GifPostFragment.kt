package social.tsu.android.ui.post.view.viewpager

import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.VideoCaptureConfig
import social.tsu.android.ui.new_post.BaseCameraFragment
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.ui.post.view.RecordingState
import social.tsu.android.utils.pickMediaFromGallery


class GifPostFragment : BaseCameraFragment<VideoCapture>() {

    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val VIDEO_EXTENSION = ".mp4"
    }

    private var recordingState: RecordingState = RecordingState.Stopped

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
}