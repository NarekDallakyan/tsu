package social.tsu.android.ui.post.view.trim

import android.net.Uri
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
import social.tsu.trimmer.TsuVideoTrimmerView
import social.tsu.trimmer.interfaces.OnTrimVideoListener
import social.tsu.trimmer.view.RangeSeekBarView


class PostTrimFragment : Fragment(), OnTrimVideoListener {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var fromScreenType: Int? = null

    // Sub views
    private lateinit var mVideoTrimmerView: TsuVideoTrimmerView
    private lateinit var mTimeLineBar: RangeSeekBarView

    private var postTypeFragment: PostTypesFragment? = null

    private var fromNext: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_trim, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialization views
        initViews()
        // Get Arguments
        getArgumentData()
        // Init view models
        initViewModels()
        // Listen on click listeners
        initOnClicks()
        // Handle trimmer preview
        handleTrimmerPreview()
    }

    private fun handleTrimmerPreview() {

        if (filePath == null) {
            Toast.makeText(requireContext(), "Can't trim this file.", Toast.LENGTH_LONG).show()
            return
        }

        // Ready to preview trimmer view
        mVideoTrimmerView.setMaxDuration(6)
        mVideoTrimmerView.setOnTrimVideoListener(this)
        mVideoTrimmerView.setVideoURI(Uri.parse(filePath))
    }

    private fun initViews() {

        mVideoTrimmerView = requireView().findViewById(R.id.videoTrimmerView)
        mTimeLineBar = requireView().findViewById(R.id.timeLineBar)
    }

    private fun initOnClicks() {

        closeLayout_id.setOnClickListener {

            fromNext = false
            mVideoTrimmerView.destroy()
            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.postTypesFragment, false)
        }

        nextLayout4_id.setOnClickListener {

            fromNext = true
            mVideoTrimmerView.onSave()
        }
    }

    override fun onStart() {
        super.onStart()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()
    }

    private fun initViewModels() {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    private fun getArgumentData() {

        if (arguments == null) return

        filePath = requireArguments().getString("filePath")
        fromScreenType = requireArguments().getInt("fromScreenType")
        postTypeFragment =
            requireArguments().getSerializable("postTypeFragment") as? PostTypesFragment?
    }

    override fun onTrimResult(uri: Uri?) {

        if (uri == null) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Oops, Can't trim this file, please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
        val filePath = uri.toString()
        requireActivity().runOnUiThread {
            mVideoTrimmerView.destroy()
            val mBundle = Bundle()
            mBundle.putString("filePath", filePath)
            mBundle.putInt("fromScreenType", fromScreenType!!)
            mBundle.putSerializable("postTypeFragment", postTypeFragment)
            sharedViewModel!!.select(false)
            findParentNavController().navigate(R.id.postPreviewFragment, mBundle)
        }
    }

    override fun onTrimCancel() {

        mVideoTrimmerView.destroy()
        requireActivity().runOnUiThread {
            Toast.makeText(
                requireContext(),
                "Oops, Can't trim this file, please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}