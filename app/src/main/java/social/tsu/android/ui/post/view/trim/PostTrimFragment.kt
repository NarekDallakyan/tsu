package social.tsu.android.ui.post.view.trim

import android.os.Bundle
import android.os.Handler
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


class PostTrimFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
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

            val videoDurationSeconds = videoTrimmerHandler.videoDurationMilliseconds / 1000

            if (videoDurationSeconds > 7 && fromScreenType == 2) {

                Toast.makeText(
                    requireContext(),
                    "Maximum duration for GIF is 7 second.",
                    Toast.LENGTH_LONG
                ).show()

                videoTrimmerHandler.onSave {
                    // Handle trim video completed
                    handleTrimVideoResult(it)
                    return@onSave
                }
            }

            videoTrimmerHandler.onSave {
                Handler().postDelayed({
                    // Handle trim video completed
                    handleTrimVideoResult(it)
                }, 1000)
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
        val filePath = it.toString()
        requireActivity().runOnUiThread {
            val mBundle = Bundle()
            mBundle.putString("filePath", filePath)
            mBundle.putInt("fromScreenType", fromScreenType!!)
            mBundle.putSerializable("postTypeFragment", postTypeFragment)
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