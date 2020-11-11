package social.tsu.android.ui.post.view.trim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_post_trim.*
import social.tsu.android.R
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.trimmer.features.trim.VideoTrimmerHandler
import social.tsu.trimmer.utils.GifUtils
import java.io.File


class PostTrimFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var originalFilePath: String? = null
    private var fromScreenType: Int? = null

    private var postTypeFragment: PostTypesFragment? = null
    private var videoTrimmerHandler = VideoTrimmerHandler()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_trim, container, false)
    }

    override fun onStart() {
        super.onStart()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Init View Models
        initViewModels()
        // Get Arguments
        getArgumentData()
        // Init on clicks
        initOnClicks()
        // Handle trimmer preview
        handleTrimmerPreview()
    }

    private fun initViewModels() {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    private fun initOnClicks() {

        closeLayout_id.setOnClickListener {

            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.postTypesFragment, false)
        }

        nextLayout4_id.setOnClickListener {

            val videoDurationSeconds = videoTrimmerHandler.videoTrimDuration

            if (videoDurationSeconds > 7 && fromScreenType == 2) {

                Toast.makeText(
                    requireContext(),
                    "Maximum duration for GIF is 7 second.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            } else if (fromScreenType == 1) {

                videoTrimmerHandler.onSave {
                    // Handle trim video completed
                    handleTrimVideoResult(it)
                }
                return@setOnClickListener
            } else if (fromScreenType == 2 && videoDurationSeconds <= 7) {
                videoTrimmerHandler.onSave {
                    // Handle trim video completed
                    handleTrimVideoResult(it)
                }
            }
        }
    }

    private fun handleTrimVideoResult(it: String?) {

        if (it == null) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Oops, Can't trim this file, please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        if (fromScreenType == 2) {

            GifUtils.convertToGif(File(filePath)) {

                if (it != null) {
                    val gifFilePath = it
                    // remove video file
                    if (File(filePath).exists()) {
                        val result = File(filePath).delete()
                        this.filePath = gifFilePath
                    }
                }
            }
        }

        if (filePath != null) {
            MainActivity.draftFiles.add(filePath!!)
        }
        if (originalFilePath != null) {
            MainActivity.draftFiles.add(originalFilePath!!)
        }

        val filePath = it.toString()
        requireActivity().runOnUiThread {
            val mBundle = Bundle()
            mBundle.putString("filePath", filePath)
            mBundle.putInt("fromScreenType", fromScreenType!!)
            mBundle.putSerializable("postTypeFragment", postTypeFragment)
            mBundle.putString("originalFilePath", originalFilePath)
            sharedViewModel!!.select(false)
            findParentNavController().navigate(R.id.postPreviewFragment, mBundle)
        }
    }

    private fun handleTrimmerPreview() {

        if (filePath == null) {
            Toast.makeText(requireContext(), "Can't trim this file.", Toast.LENGTH_LONG).show()
            return
        }

        // Starting preview trimmer view
        videoTrimmerHandler.initUI(trimmer_view, filePath, requireContext())
    }

    private fun getArgumentData() {

        if (arguments == null) return

        filePath = requireArguments().getString("filePath")
        originalFilePath = requireArguments().getString("originalFilePath")
        fromScreenType = requireArguments().getInt("fromScreenType")
        postTypeFragment =
            requireArguments().getSerializable("postTypeFragment") as? PostTypesFragment?
    }

    override fun onPause() {
        super.onPause()
        videoTrimmerHandler.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoTrimmerHandler.onDestroy()
    }
}