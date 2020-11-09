package social.tsu.android.ui.post.view.preview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_post_preview.*
import social.tsu.android.R
import social.tsu.android.ext.addOnKeyboardListener
import social.tsu.android.ext.hide
import social.tsu.android.ext.hideKeyboard
import social.tsu.android.ext.show
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.post.model.ColorModel
import social.tsu.android.ui.post.model.FontModel
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.KeyboardListener
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.overlay.PreviewVideoActivity


class PostPreviewFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var originalFilePath: String? = null
    private var fromScreenType: Int? = null

    // Sub views
    private var imagePreview: ImageView? = null
    private var videoPreview: VideoView? = null
    private var postTypeFragment: PostTypesFragment? = null
    private var originalMode: Int? = null

    // fonts and colors adapters
    private val fontModels: ArrayList<FontModel> by lazy {
        val fontList = arrayListOf<FontModel>()
        fontList.add(FontModel(R.drawable.ic_font_1_drawable, false))
        fontList.add(FontModel(R.drawable.ic_font_3_drawable, false))
        fontList.add(FontModel(R.drawable.ic_font_2_drawable))
        fontList.add(FontModel(R.drawable.ic_font_4_drawable))
        fontList.add(FontModel(R.drawable.ic_font_5_drawable))
        fontList.add(FontModel(R.drawable.ic_font_6_drawable))
        fontList.add(FontModel(R.drawable.ic_font_1_drawable))
        fontList.add(FontModel(R.drawable.ic_font_3_drawable))
        fontList.add(FontModel(R.drawable.ic_font_2_drawable))
        fontList.add(FontModel(R.drawable.ic_font_4_drawable))
        fontList.add(FontModel(R.drawable.ic_font_5_drawable))
        fontList.add(FontModel(R.drawable.ic_font_6_drawable))
        return@lazy fontList
    }

    private val colorModels: ArrayList<ColorModel> by lazy {
        val colorList = arrayListOf<ColorModel>()
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        colorList.add(ColorModel())
        return@lazy colorList
    }

    override fun onDestroy() {
        super.onDestroy()
        originalMode?.let { activity?.window?.setSoftInputMode(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        originalMode = activity?.window?.attributes?.softInputMode
        activity?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

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
        // Init adapters
        initAdapters()
    }

    override fun onStart() {
        super.onStart()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()
    }

    private fun previewFile() {

        if (fromScreenType == 0) {
            // File type is image
            videoPreview?.visibility = View.GONE
            imagePreview?.visibility = View.VISIBLE
            imagePreview?.setImageURI(Uri.parse(filePath))
        } else {
            // File type is video
            videoPreview?.visibility = View.VISIBLE
            imagePreview?.visibility = View.GONE
            videoPreview?.setVideoURI(Uri.parse(filePath))
            val mediaController = MediaController(requireContext())
            videoPreview?.setMediaController(mediaController)
            videoPreview?.start()
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
        postTypeFragment =
            requireArguments().getSerializable("postTypeFragment") as? PostTypesFragment?
        originalFilePath = requireArguments().getString("originalFilePath")
    }

    private fun initViewModels() {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    private fun initOnClicks() {

        // Back button clicked
        previewBackBtn.setOnClickListener {

            if (keyboardContainer.visibility == View.VISIBLE) {
                requireView().hideKeyboard(requireActivity())
                return@setOnClickListener
            }

            val mainActivity = requireActivity() as? MainActivity
            mainActivity?.supportActionBar?.hide()


            if (fromScreenType == 0 || fromScreenType == 1) {
                // Back to trim fragment
                sharedViewModel?.select(false)
                findParentNavController().popBackStack(R.id.postTypesFragment, false)
            } else {
                // Back to post type fragment
                sharedViewModel?.select(false)
                findParentNavController().popBackStack(R.id.postTrimFragment, false)
            }
        }

        // Post file clicked
        postFile.setOnClickListener {

            val mainActivity = requireActivity() as? MainActivity
            mainActivity?.supportActionBar?.show()

            (postTypeFragment)?.next(
                videoPath = filePath,
                originalFilePath = originalFilePath,
                fromGrid = true
            )
        }

        // Add text click listener
        addTextLayout.setOnClickListener {

            handleAddTextClicked()
        }

        // Listen keyboard visibility listener
        requireActivity().addOnKeyboardListener(object : KeyboardListener {
            override fun onKeyboardHidden() {
                keyboardContainer?.hide(animate = true, duration = 200)
                addTextLayout?.show()
            }

            override fun onKeyboardShown() {
                keyboardContainer?.show(animate = true, duration = 500)
                addTextLayout?.hide()
            }
        })
    }

    private fun handleAddTextClicked() {

        val intent = Intent(requireContext(), PreviewVideoActivity::class.java)
        intent.putExtra("DATA", filePath)
        startActivity(intent)
    }

    private fun initAdapters() {

        val fontsAdapter = FontsAdapter()
        val colorsAdapter = ColorAdapter()
        fontsAdapter.submitList(fontModels)
        colorsAdapter.submitList(colorModels)
        fontsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        fontsRecyclerView.adapter = fontsAdapter
        colorsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        colorsRecyclerView.adapter = colorsAdapter
    }
}