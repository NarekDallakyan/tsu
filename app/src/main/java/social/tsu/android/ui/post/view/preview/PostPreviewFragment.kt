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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_post_preview.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.ext.*
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.post.helper.PostPreviewUiHelper
import social.tsu.android.ui.post.model.ColorModel
import social.tsu.android.ui.post.model.FontModel
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.KeyboardListener
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.overlay.view.OverlayHandler
import social.tsu.trimmer.utils.GifUtils
import java.io.File


class PostPreviewFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var originalFilePath: String? = null
    private var fromScreenType: Int? = null

    // Sub views
    private var imagePreview: ImageView? = null
    private var originalMode: Int? = null

    // Helper
    private val previewUiHelper = PostPreviewUiHelper

    // Text overlay
    private lateinit var overlayHandler: OverlayHandler

    private var activeColor: ColorModel.ColorEnum = ColorModel.ColorEnum.White
    private var activeFont: Typeface? =
        Typeface.createFromAsset(TsuApplication.mContext.assets, "classic.ttf")

    private var selectedColorItem: Int = -1
    private var selectedFontItem: Int = 2

    private lateinit var fontsAdapter: FontsAdapter
    private lateinit var colorsAdapter: ColorAdapter

    override fun onDestroy() {
        super.onDestroy()
        originalMode?.let { activity?.window?.setSoftInputMode(it) }
        overlayHandler.destroy()
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
        // Init font adapter
        initFontAdapter()
        // Init color adapter
        initColorAdapter()
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
        // invalidate text overlay
        invalidateOverlay()
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


            if (fromScreenType == 0) {
                // Back to trim fragment
                sharedViewModel?.select(false)
                findParentNavController().navigate(R.id.postTypesFragment)
            } else {
                // Back to post type fragment
                sharedViewModel?.select(false)
                findParentNavController().popBackStack(R.id.postTrimFragment, false)
            }
        }

        // Post file clicked
        postFile.setOnClickListener {

            handleNext()
        }

        // Add text click listener
        textAction.setOnClickListener {

            requireView().showKeyboard(requireActivity())
        }

        // Listen keyboard visibility listener
        requireActivity().addOnKeyboardListener(object : KeyboardListener {
            override fun onKeyboardHidden() {
                if (view == null) return
                handleKeyboardHidden()
            }

            override fun onKeyboardShown() {
                if (view == null) return
                handleKeyboardShown()
            }
        })

        // listen overlay done click
        textOverlayDone.setOnClickListener {

            if (activeFont != null && add_text_edit_text?.text != null) {

                requireView().hideKeyboard(requireActivity())

                val isWatermarkOn = fontsAdapter.getData()[0].watermark

                overlayHandler.onDoneClicked(
                    activeFont,
                    Color.parseColor(activeColor.value),
                    activeColor.name,
                    isWatermarkOn,
                    add_text_edit_text?.text.toString()
                )
            }
        }
    }

    private fun handleKeyboardShown() {

        keyboardContainer?.show(animate = true, duration = 500)
        actionsLayout?.hide(animate = true, duration = 200)
        textOverlayDone?.show(animate = true, duration = 500)
        add_text_edit_text?.show(animate = true, duration = 500)
        add_text_edit_text?.requestFocus()
        postFile?.hide(animate = true, duration = 200)
        previewBackBtn?.hide(animate = true, duration = 200)
        previewTitle?.hide(animate = true, duration = 200)
    }

    private fun handleKeyboardHidden() {

        keyboardContainer?.hide(animate = true, duration = 200)
        actionsLayout?.show(animate = true, duration = 500)
        textOverlayDone?.hide(animate = true, duration = 200)
        add_text_edit_text?.clearFocus()
        add_text_edit_text?.hide(animate = true, duration = 200)
        postFile?.show(animate = true, duration = 500)
        previewBackBtn?.show(animate = true, duration = 500)
        previewTitle?.show(animate = true, duration = 500)
        // invalidate text overlay
        invalidateOverlay()
    }

    /**
     * Clear text overlay parameters
     */
    private fun invalidateOverlay() {

        activeFont = Typeface.createFromAsset(TsuApplication.mContext.assets, "classic.ttf")
        activeColor = ColorModel.ColorEnum.White
        overlayHandler.invalidateTextOverlay(activeFont, Color.parseColor(activeColor.value))
        initColorAdapter()
        initFontAdapter()
    }

    private fun handleNext() {

        filePreviewLoader.show()
        overlayHandler.saveOverlay(object : OverlayHandler.OverlayListener {
            override fun onSave(filePath: String?) {
                filePreviewLoader.hide()

                if (fromScreenType == 2) {

                    GifUtils.convertToGif(File(filePath)) {

                        if (it != null) {
                            val gifFilePath = it
                            // remove video file
                            if (File(filePath).exists()) {
                                val result = File(filePath).delete()
                                (PostTypesFragment.instance()).next(
                                    videoPath = gifFilePath,
                                    originalFilePath = originalFilePath,
                                    fromGrid = true,
                                    screenPosition = fromScreenType
                                )
                                overlayHandler.destroy()
                            }
                        }
                    }
                    return
                }

                (PostTypesFragment.instance()).next(
                    videoPath = filePath,
                    originalFilePath = originalFilePath,
                    fromGrid = true,
                    screenPosition = fromScreenType
                )
                overlayHandler.destroy()
            }

            override fun onError() {
                filePreviewLoader.hide()
                Toast.makeText(requireContext(), "Saving failure.", Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     *  Font adapter for Text overlay adapters
     */
    private fun initFontAdapter() {

        fontsAdapter = FontsAdapter()

        fontsAdapter.addItemClickListener { position, itemModel ->
            handleFontItemClicked(position, itemModel, fontsAdapter)
        }
        fontsAdapter.submitList(previewUiHelper.getFontList(requireContext()))

        fontsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        fontsRecyclerView.adapter = fontsAdapter
    }

    /**
     *  Color adapter for Text overlay adapters
     */
    private fun initColorAdapter() {

        colorsAdapter = ColorAdapter()
        colorsAdapter.addItemClickListener { position, itemModel ->

            if (this.selectedColorItem >= 0) {
                colorsAdapter.getData()[this.selectedColorItem].isSelected = false
                colorsAdapter.notifyItemChanged(this.selectedColorItem)
            }

            colorsAdapter.getData()[position].isSelected = true
            colorsAdapter.notifyItemChanged(position)
            this.selectedColorItem = position

            activeColor = itemModel.color
            overlayHandler.colorItemClicked(
                Color.parseColor(itemModel.color.value),
                activeColor.name,
                fontsAdapter.getData()[0].watermark
            )
        }
        colorsAdapter.submitList(previewUiHelper.getColorList())
        colorsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        colorsRecyclerView.adapter = colorsAdapter
    }

    private fun handleFontItemClicked(
        position: Int,
        itemModel: FontModel,
        fontsAdapter: FontsAdapter
    ) {


        when (itemModel.itemType) {

            FontModel.ItemType.WATERMARK -> {

                val isWatermarkOn = itemModel.watermark
                overlayHandler.watermark(
                    isWatermarkOn,
                    Color.parseColor(activeColor.value),
                    activeColor.name
                )
            }

            FontModel.ItemType.FONT -> {

                activeFont = itemModel.font
                if (selectedFontItem >= 2) {
                    fontsAdapter.getData()[this.selectedFontItem].isSelected = false
                    fontsAdapter.notifyItemChanged(this.selectedFontItem)
                }
                fontsAdapter.getData()[position].isSelected = true
                fontsAdapter.notifyItemChanged(position)
                this.selectedFontItem = position
                overlayHandler.fontItemClicked(itemModel.font)
            }

            FontModel.ItemType.ALIGN -> {

                val gravity = itemModel.align
                overlayHandler.changeTextGravity(gravity)
            }
        }
    }
}