package social.tsu.android.ui.post.view.preview

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.api.load
import kotlinx.android.synthetic.main.fragment_post_preview.*
import social.tsu.android.R
import social.tsu.android.ext.getRealPathFromURI
import social.tsu.android.ui.MainActivity
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel


class PostPreviewFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var fromScreenType: Int? = null

    // Sub views
    private var imagePreview: ImageView? = null
    private var videoPreview: VideoView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialization views
        initViews()
        // Get argument data
        getArgumentData()
        // Init view models
        initViewModels()
        // Init on clicks
        initOnClicks()
        // Preview file
        previewFile()
    }

    private fun previewFile() {

        if (fromScreenType == 0) {
            // File type is image
            videoPreview?.visibility = View.GONE
            imagePreview?.visibility = View.VISIBLE
            imagePreview?.load(Uri.parse("file://".plus(filePath)))
        } else {
            // File type is video
            videoPreview?.visibility = View.VISIBLE
            imagePreview?.visibility = View.GONE
            videoPreview?.setVideoURI(Uri.parse(filePath))
        }
    }

    private fun initViews() {

        imagePreview = view?.findViewById(R.id.imagePreview)
        videoPreview = view?.findViewById(R.id.filePreview)
    }

    private fun getArgumentData() {

        if (arguments == null) return

        filePath = requireArguments().getString("filePath")
        fromScreenType = requireArguments().getInt("fromScreenType")
    }

    private fun initViewModels() {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    private fun initOnClicks() {

        // Back button clicked
        previewBackBtn.setOnClickListener {

            // Back to trim fragment
            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.postTrimFragment, false)
        }

        // Post file clicked
        postFile.setOnClickListener {

            Toast.makeText(requireContext(), "Post", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.show()
    }
}