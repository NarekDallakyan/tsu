package social.tsu.android.ui.post.view.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import social.tsu.android.R
import social.tsu.android.ui.post.helper.CameraHelper
import social.tsu.camerarecorder.widget.Filters


class PhotoCameraPostFragment : Fragment() {

    private var cameraHelper: CameraHelper? = null

    fun handleOnResume() {
        // Initialize camera helper
        cameraHelper = CameraHelper(requireActivity(), requireContext(), requireView())
        cameraHelper?.onResume()
    }

    fun handleOnStop() {
        cameraHelper?.onStop()
    }

    fun handleFilter(filters: Filters) {
        cameraHelper?.changeFilter(filters)
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
        //setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        return inflater.inflate(R.layout.fragment_camera_post, container, false)
    }

    fun switchCamera() {

        cameraHelper?.switchCamera()
    }

    fun handleFlash() {
        cameraHelper?.handleFlash()
    }

    fun capturePicture(function: ((String?) -> Unit)? = null) {

        if (cameraHelper?.isCapturing() == true) {
            return
        }
        cameraHelper?.capturePicture(function)
    }
}