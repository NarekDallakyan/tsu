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
import social.tsu.android.ui.post.view.trim.features.trim.VideoTrimmerActivity
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel


class PostTrimFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var fromScreenType: Int? = null

    private var postTypeFragment: PostTypesFragment? = null

    private var fromNext: Boolean = false
    private var videoTrimmerActivity = VideoTrimmerActivity()


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

            videoTrimmerActivity.onSave {

                // Handle trim video completed
                handleTrimVideoResult(it)
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
        videoTrimmerActivity.initUI(trimmer_view, filePath, requireContext())
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
        videoTrimmerActivity.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoTrimmerActivity.onDestroy()
    }
}