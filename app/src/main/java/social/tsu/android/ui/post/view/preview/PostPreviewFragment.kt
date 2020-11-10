package social.tsu.android.ui.post.view.preview

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_post_preview.*
import social.tsu.android.R
import social.tsu.android.ext.*
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.post.helper.PostPreviewUiHelper
import social.tsu.android.ui.post.model.FontModel
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.KeyboardListener
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.overlay.view.OverlayHandler


class PostPreviewFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var originalFilePath: String? = null
    private var fromScreenType: Int? = null

    // Sub views
    private var imagePreview: ImageView? = null
    private var postTypeFragment: PostTypesFragment? = null
    private var originalMode: Int? = null

    // Helper
    private val previewUiHelper = PostPreviewUiHelper

    // Text overlay
    private lateinit var overlayHandler: OverlayHandler

    private var activeColor: Int? = null
    private var activeFont: Typeface? = null

    override fun onDestroy() {
        super.onDestroy()
        originalMode?.let { activity?.window?.setSoftInputMode(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayHandler = OverlayHandler()
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
        // Init overlay helper
        initOverlayHelper()
    }

    private fun initOverlayHelper() {

        overlayHandler.initialize(
            requireActivity() as AppCompatActivity,
            requireContext(),
            ivImage,
            videoSurface,
            add_text_edit_text
        )
        overlayHandler.onCreate(filePath)
    }

    override fun onStart() {
        super.onStart()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()
    }

    private fun previewFile() {

        if (fromScreenType == 0) {
            // File type is image
            imagePreview?.visibility = View.VISIBLE
            imagePreview?.setImageURI(Uri.parse(filePath))
        } else {
            // File type is video
            imagePreview?.visibility = View.GONE
        }
    }

    /**
     *  Initialize views
     */
    private fun initViews() {

        imagePreview = view?.findViewById(R.id.imagePreview)
        add_text_edit_text.clearFocus()
    }

    /**
     *  Get argument data from bundles
     */
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
        textAction.setOnClickListener {

            requireView().showKeyboard(requireActivity())
        }

        // Listen keyboard visibility listener
        requireActivity().addOnKeyboardListener(object : KeyboardListener {
            override fun onKeyboardHidden() {
                keyboardContainer?.hide(animate = true, duration = 200)
                actionsLayout?.show(animate = true, duration = 500)
                textOverlayDone?.hide(animate = true, duration = 200)
                add_text_edit_text.clearFocus()
                add_text_edit_text.hide(animate = true, duration = 200)
                postFile.show(animate = true, duration = 500)
                previewBackBtn.show(animate = true, duration = 500)
                previewTitle.show(animate = true, duration = 500)
            }

            override fun onKeyboardShown() {
                keyboardContainer?.show(animate = true, duration = 500)
                actionsLayout?.hide(animate = true, duration = 200)
                textOverlayDone?.show(animate = true, duration = 500)
                add_text_edit_text?.show(animate = true, duration = 500)
                add_text_edit_text.requestFocus()
                postFile.hide(animate = true, duration = 200)
                previewBackBtn.hide(animate = true, duration = 200)
                previewTitle.hide(animate = true, duration = 200)
            }
        })

        // listen overlay done click
        textOverlayDone.setOnClickListener {

            if (activeFont != null && activeColor != null && add_text_edit_text.text != null) {

                requireView().hideKeyboard(requireActivity())
                overlayHandler.onDoneClicked(
                    activeFont,
                    activeColor!!,
                    add_text_edit_text.text.toString()
                )
            }
        }
    }

    /**
     *  Text overlay adapters (Fonts and Colors)
     */
    private fun initAdapters() {

        val fontsAdapter = FontsAdapter()
        val colorsAdapter = ColorAdapter()
        colorsAdapter.addItemClickListener { position, itemModel ->
            activeColor = itemModel.color
            overlayHandler.colorItemClicked(itemModel.color)
        }
        fontsAdapter.addItemClickListener { position, itemModel ->
            handleFontItemClicked(position, itemModel)
        }
        fontsAdapter.submitList(previewUiHelper.getFontList(requireContext()))
        colorsAdapter.submitList(previewUiHelper.getColorList())
        fontsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        fontsRecyclerView.adapter = fontsAdapter
        colorsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        colorsRecyclerView.adapter = colorsAdapter
    }

    private fun handleFontItemClicked(position: Int, itemModel: FontModel) {

        activeFont = itemModel.font

        when (itemModel.itemType) {

            FontModel.ItemType.WATERMARK -> {

                val isWatermarkOn = itemModel.watermark
                overlayHandler.watermark(isWatermarkOn, activeColor ?: Color.WHITE)
            }

            FontModel.ItemType.FONT -> {
                overlayHandler.fontItemClicked(itemModel.font)
            }

            FontModel.ItemType.ALIGN -> {

            }
        }
    }
}