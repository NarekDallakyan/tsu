package social.tsu.android.ui.post.view.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import social.tsu.android.R
import social.tsu.android.ui.post.helper.CameraHelper

class RecordVideoPostFragment : Fragment() {

    private var cameraHelper: CameraHelper? = null

    private var filePath: String? = null

    fun handleOnResume() {
        // Initialize camera helper
        cameraHelper = CameraHelper(requireActivity(), requireContext(), requireView())
        cameraHelper?.onResume()
    }

    fun handleOnStop() {
        cameraHelper?.onStop()
    }

    override fun onStop() {
        super.onStop()
        cameraHelper?.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video_post_fraagment, container, false)
    }

    fun switchCamera() {

        cameraHelper?.switchCamera()
    }

    fun handleFlash() {
        cameraHelper?.handleFlash()
    }

    fun recordVideo(function: (onCancel: Boolean, onStart: Boolean) -> Unit) {

        if (cameraHelper?.isRecording() == true) {

            filePath = cameraHelper?.getVideoFilePath()
            filePath?.let {
                cameraHelper?.startRecording(it)
                function(false, true)
            }
        }
    }

    fun stopRecording() {

        cameraHelper?.stopRecording()
    }
}