package social.tsu.android.ui.post.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_post_types.*
import social.tsu.android.R
import social.tsu.android.TsuApplication.Companion.filterItems
import social.tsu.android.ext.hide
import social.tsu.android.ext.onSwipeListener
import social.tsu.android.ext.show
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.post.helper.PostTypeUiHelper
import social.tsu.android.ui.post.model.FilterVideoModel
import social.tsu.android.ui.post.view.filter.FilterVideoAdapter
import social.tsu.android.ui.post.view.viewpager.GifPostHandler
import social.tsu.android.ui.post.view.viewpager.PhotoCameraPostHandler
import social.tsu.android.ui.post.view.viewpager.RecordVideoPostHandler
import social.tsu.android.ui.post.view.viewpager.TsuViewPager
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.cameracapturer.filter.Filter
import social.tsu.trimmer.features.trim.VideoTrimmerUtil
import java.io.Serializable

/**
 * Main entry point for choosing media that can be attached to post.
 * All params and settings can be passed through navArgs:
 * - Allow videos
 * - defaultPostType - controls which fragment will be preselected on start. Used when obtaining
 * particular media type(image or video) to point user to correct fragment
 * - popToDestination - destination to go to when media selection/creation is finished
 * - postText - text for post. Not used here and will be passed to PostDraftFragment after media selection.
 * Used to save text previously entered by user
 * - recipient - recipient for post, used when creating message, optional
 * - postingType - {@link social.tsu.android.ui.post.view.draft.PostDraftType} defaults to POST. May be POST, MESSAGE, COMMUNITY
 * - membership - used when posting to community, optional
 *
 * In order to add new source for media you need to add new fragment to post_types_graph.xml and
 * add menu item to post_bottom_menu.xml with android:id=<your_fragment_id_in_navgraph>
 * You can extend {@link BaseCameraFragment} which provides camera initialization routines, or
 * create completely custom fragment if you need more customization
 */

open class PostTypesFragment : Fragment(), Serializable {

    private val args: PostTypesFragmentArgs by navArgs()

    val allowVideo by lazy { args.allowVideo }

    var sharedViewModel: SharedViewModel? = null

    /**
     *  Return screen type
     *
     *  - 0 value is Photo section
     *  - 1 value is Video section
     *  - 2 value is Gif section
     */
    private var position: Int = 0

    private var enableSwiping = true

    private lateinit var photoCameraPostHandler: PhotoCameraPostHandler
    private lateinit var recordVideoPostHandler: RecordVideoPostHandler
    private lateinit var gifPostHandler: GifPostHandler

    private val postTypeUiHelper = PostTypeUiHelper

    // File variables
    private var filePath: String? = null

    // Filters
    private var filterVideoAdapter: FilterVideoAdapter? = null
    private var selectedFilterItemPosition: Int = -1

    /**
     *  Handling record mode UI
     */
    private fun recordingMode(recording: Boolean) {

        if (recording) {

            camera_rotate_id?.hide(animate = true)
            post_bottom_navbar?.hide(invisible = true, animate = true)
            enableSwiping = false
            snap_icon_id.setImageResource(R.drawable.video_record_start)
            sectionsLayout?.hide(animate = true)
            optionsLayout?.hide(animate = true)
            gallery_image_id?.hide(animate = true)
        } else {

            snap_icon_id.setImageResource(R.drawable.record_video_not_start)
            enableSwiping = true
            sectionsLayout?.show(animate = true)
            optionsLayout?.show(animate = true)
            gallery_image_id?.show(animate = true)
            camera_rotate_id?.show(animate = true)
            post_bottom_navbar?.show(animate = true)
        }

    }

    override fun onStop() {
        super.onStop()
        // remove recording mode
        recordingMode(false)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_types, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // init handlers
        initHandlers()
        recordingMode(false)
        // Init view models
        initViewModels()
        // Ui initialization
        initUi()
        // Init on click listeners
        initOnClicks()
    }

    private fun initHandlers() {

        if (wrap_view == null) return
        photoCameraPostHandler = PhotoCameraPostHandler(wrap_view!!, this, requireContext())
        recordVideoPostHandler = RecordVideoPostHandler(wrap_view!!, this, requireContext())
        gifPostHandler = GifPostHandler(wrap_view!!, this, requireContext())
        photoCameraPostHandler.initialize()
        recordVideoPostHandler.initialize()
        gifPostHandler.initialize()
    }

    override fun onStart() {
        super.onStart()
        // Handle view pager after changing
        postTypeUiHelper.handleViewPagerChange(
            requireContext(),
            0,
            view
        )
    }


    private fun initOnClicks() {

        // Listen camera switch button on click
        camera_rotate_id.setOnClickListener {

            handleSwitchCamera()
        }

        // Listen start camera button on click
        snap_icon_id.setOnClickListener {

            handleStartCamera()
        }

        // Listen library button on click
        gallery_image_id.setOnClickListener {

            val mBundle = Bundle()
            mBundle.putSerializable("postTypeFragment", this)
            findParentNavController().navigate(R.id.mediaLibraryLayout_id, mBundle)
        }

        // Listen filter close layout on click
        filterCancel?.setOnClickListener {

            handleFilterMode(true)
        }

        // Listen apply filter on click
        filterDone?.setOnClickListener {

            handleFilterMode(applyFilter = true)
        }

        // Listen apply flash on click
        flashLayout_id?.setOnClickListener {

            // Handling camera flash mode functionality
            when (position) {
                0 -> {
                    photoCameraPostHandler.handleFlash()
                }
                1 -> {
                    recordVideoPostHandler.handleFlash()
                }
                else -> {
                    gifPostHandler.handleFlash()
                }
            }

            handleFlashIcon()
        }

        // Listen apply speed icon in click
        speedLayout?.setOnClickListener {

            handleSpeedIcon()
        }

        // Listen sound button on click
        soundLayout_id?.setOnClickListener {
            // Changing layout alpha after clicking

            handleSoundIcon()
        }

        // Listen language button on click
        languageLayout_id?.setOnClickListener {
            postTypeUiHelper.setChoose(
                LANGUAGE_CLICK,
                view
            )
        }

        photoLayout_id?.setOnClickListener {
            postTypeUiHelper.setChoose(
                PHOTO_CLICK,
                view
            )
        }

        wifiLayout_id?.setOnClickListener {
            postTypeUiHelper.setChoose(
                WIFI_CLICK,
                view
            )
        }

        closeLayout_id?.setOnClickListener {
            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.mainFeedFragment, false)
        }

        filterLayout_id?.setOnClickListener {

            // Handle filter clicked
            handleFilterMode()
        }
    }

    /**
     *  Initialize and start camera
     */
    private fun handleStartCamera() {

        // Get view pager current page position
        when (position) {
            0 -> {

                handlePhotoCaptureClicked(photoCameraPostHandler)
            }
            1 -> {

                handleVideoRecordClicked(recordVideoPostHandler)
            }
            else -> {

                handleRecordGifClicked(gifPostHandler)
            }
        }
    }

    /**
     *  Changing Ui when user click gif record button and handle this functionality
     */
    private fun handleRecordGifClicked(handler: GifPostHandler) {

        if (gifPostHandler.isRecording()) {
            gifPostHandler.stopRecordGif {
                navigateTrimFromGifSectionScreen(it)
            }
        } else {
            recordingMode(true)
            handler.recordGif()
        }
    }

    private fun navigateTrimFromGifSectionScreen(it: String) {

        this.filePath = it
        MainActivity.draftFiles.add(filePath!!)
        val mBundle = Bundle()
        mBundle.putString("filePath", filePath)
        mBundle.putString("originalFilePath", filePath)
        mBundle.putInt("fromScreenType", position)
        mBundle.putSerializable("postTypeFragment", this)
        sharedViewModel!!.select(false)
        // Configure trimmer video limitation
        VideoTrimmerUtil.TYPE = 2
        findParentNavController().navigate(R.id.postTrimFragment, mBundle)
    }

    /**
     *  Changing Ui when user click video record button and handle this functionality
     */
    private fun handleVideoRecordClicked(handler: RecordVideoPostHandler) {

        if (recordVideoPostHandler.isRecording()) {
            recordVideoPostHandler.stopRecording {
                navigateTrimFromVideoSectionScreen(it)
            }
        } else {
            recordingMode(true)
            handler.recordVideo()
        }
    }

    private fun navigateTrimFromVideoSectionScreen(it: String) {

        this.filePath = it
        MainActivity.draftFiles.add(filePath!!)
        val mBundle = Bundle()
        mBundle.putString("filePath", filePath)
        mBundle.putString("originalFilePath", filePath)
        mBundle.putInt("fromScreenType", position)
        mBundle.putSerializable("postTypeFragment", this)
        sharedViewModel?.select(false)
        // Configure trimmer video limitation
        VideoTrimmerUtil.TYPE = 1
        findParentNavController().navigate(R.id.postTrimFragment, mBundle)
    }

    /**
     *  Changing Ui when user click capture button and handle this functionality
     */
    private fun handlePhotoCaptureClicked(handler: PhotoCameraPostHandler) {

        handler.capturePicture {
            this.filePath = it

            if (this.filePath == null) {
                Toast.makeText(
                    requireContext(),
                    "Can not continue, file is empty.",
                    Toast.LENGTH_LONG
                ).show()
                return@capturePicture
            }

            MainActivity.draftFiles.add(filePath!!)
            next(
                photoUri = Uri.parse(filePath),
                originalFilePath = filePath,
                fromGrid = true
            )
        }
    }

    /**
     *  Changing Camera id when user click camera switch button
     */
    private fun handleSwitchCamera() {

        // Get view pager current page position
        when (position) {
            0 -> {
                photoCameraPostHandler.switchCamera()
            }
            1 -> {
                recordVideoPostHandler.switchCamera()
            }
            else -> {
                gifPostHandler.switchCamera()
            }
        }
    }

    private fun initUi() {

        // listen camera view swipe
        wrap_view?.onSwipeListener {

            if (!enableSwiping) {
                return@onSwipeListener
            }

            if (!it) {
                if (position < 2) {
                    position++
                }
            } else {
                if (position > 0) {
                    position--
                }
            }

            // Handle view pager after changing
            postTypeUiHelper.handleViewPagerChange(
                requireContext(),
                position,
                view
            )

            handleSpeedIcon(false)
            handleFlashIcon(false)
            handleSoundIcon(true)
        }
        // Set default Camera Mode
        postTypeUiHelper.setChoose(
            PHOTO_CLICK,
            view
        )
        // Hide tool bar
        post_types_toolbar.visibility = View.GONE
    }

    /**
     *  Initialize view models
     */
    private fun initViewModels() {
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    /**
     *  Handling Sound icon visibility
     */
    private fun handleSoundIcon(enable: Boolean? = null) {

        var soundTag = soundIcon?.tag as? String

        if (enable != null) {

            soundTag = if (enable) {
                "off"
            } else {
                "on"
            }
        }

        if (soundTag == "on") {
            soundIcon?.setImageResource(R.drawable.sound_off)
            soundIcon?.tag = "off"
        } else {
            soundIcon?.setImageResource(R.drawable.sound_on)
            soundIcon?.tag = "on"
        }
    }

    private fun handleSpeedIcon(enable: Boolean? = null) {

        var speedTag = speedIcon?.tag as? String
        if (enable != null) {

            speedTag = if (enable) {
                "off"
            } else {
                "on"
            }
        }

        if (speedTag == "on") {
            speedIcon?.setImageResource(R.drawable.ic_speed_off)
            speedIcon?.tag = "off"
        } else {
            speedIcon?.setImageResource(R.drawable.ic_speed_on)
            speedIcon?.tag = "on"
        }
    }

    /**
     *  Handling Flash icon visibility
     */
    private fun handleFlashIcon(enable: Boolean? = null) {

        var flashTag = flashIcon?.tag as? String

        if (enable != null) {

            flashTag = if (enable) {
                "off"
            } else {
                "on"
            }
        }

        if (flashTag == "on") {
            flashIcon?.setImageResource(R.drawable.ic_flash_offwhite)
            flashIcon?.tag = "off"
        } else {
            flashIcon?.setImageResource(R.drawable.flash_on)
            flashIcon?.tag = "on"
        }
    }

    /**
     *  Changing Ui when user click filter section
     */
    private fun handleFilterMode(hideFilter: Boolean = false, applyFilter: Boolean = false) {

        selectedFilterItemPosition = -1

        var filterLayoutIsVisible = isFilterPending()

        if (hideFilter) {
            filterLayoutIsVisible = true
        }

        if (applyFilter) {
            filterLayoutIsVisible = true
        }

        if (!filterLayoutIsVisible) {

            // Init filter adapter
            initFilterAdapter()
            // Visible filter list layout
            filterListLayout?.show(animate = true, duration = 500)
            post_bottom_navbar?.hide()
            snap_icon_id?.hide()
            gallery_image_id?.hide()
            camera_rotate_id?.hide()
            (newPostViewPager as? TsuViewPager)?.enableSwiping(false)
        } else {

            // Gone filter list layout
            filterNameLayout?.hide()
            filterListLayout?.hide()
            post_bottom_navbar?.show(animate = true, duration = 500)
            snap_icon_id?.show(animate = true, duration = 500)
            gallery_image_id?.show(animate = true, duration = 500)
            camera_rotate_id?.show(animate = true, duration = 500)
            (newPostViewPager as? TsuViewPager)?.enableSwiping(true)
            if (!applyFilter) {
                changeCameraFilter((filterVideoAdapter?.getData()?.get(0)?.filterObject as Filter))
            }
        }
    }

    /**
     *  Initialize filter recycler view
     */
    private fun initFilterAdapter() {

        filterRecyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        filterVideoAdapter = FilterVideoAdapter()

        filterVideoAdapter?.addItemClickListener { position: Int, itemModel: FilterVideoModel ->

            // Handle filter item clicked
            filterNameLayout?.show(animate = true, duration = 500)
            handleFilterItemClicked(position, itemModel)
        }
        filterVideoAdapter?.submitList(filterItems)
        filterRecyclerView?.adapter = filterVideoAdapter
    }

    /**
     *  Handling when user click filter section
     */
    private fun handleFilterItemClicked(position: Int, itemModel: FilterVideoModel) {

        val currentItems = filterVideoAdapter?.getData() ?: return

        if (selectedFilterItemPosition > -1) {
            currentItems[selectedFilterItemPosition].select(false)
            filterVideoAdapter?.notifyItemChanged(selectedFilterItemPosition)
        }
        currentItems[position].select(true)
        filterVideoAdapter?.notifyItemChanged(position)

        selectedFilterItemPosition = position

        filterNameBuble?.text = "filter"
        changeCameraFilter((itemModel.filterObject as Filter))
    }

    /**
     *  Return is filter mode
     */
    private fun isFilterPending(): Boolean = filterListLayout?.visibility == VISIBLE

    /**
     *  Changing Camera filter
     */
    private fun changeCameraFilter(filters: Filter) {

        return when (position) {
            0 -> {
                photoCameraPostHandler.handleFilter(filters)
            }
            1 -> {
                recordVideoPostHandler.handleFilter(filters)
            }
            else -> {
                gifPostHandler.handleFilter(filters)
            }
        }
    }

    override fun onDestroyView() {
        post_types_toolbar.visibility = View.GONE
        super.onDestroyView()
        sharedViewModel!!.select(false)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            sharedViewModel!!.select(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Proceed with captured media.
     * Will pass any created|chosen media to PostDraftFragment via "next" action from nav graph
     * PostDraftFragment will handle available URIs and displey media preview in post editor
     * This function is called from child fragment when it is finished with capture and has ready-to-use file
     * that needs to be passed to editor
     */
    fun next(
        videoPath: String? = null,
        videoContentUri: String? = null,
        photoUri: Uri? = null,
        fromGrid: Boolean? = null,
        originalFilePath: String? = null
    ) {

        if (fromGrid == true) {

            val mBundle = Bundle()
            mBundle.putString("videoPath", videoPath)
            mBundle.putString("originalFilePath", originalFilePath)
            mBundle.putString("videoContentUri", videoContentUri)
            mBundle.putString("photoUri", photoUri?.toString())
            mBundle.putString("postText", args.postText)
            mBundle.putParcelable("recipient", args.recipient)
            mBundle.putSerializable("postingType", args.postingType)
            mBundle.putParcelable("membership", args.membership)
            mBundle.putBoolean("allowVideo", args.allowVideo)
            mBundle.putInt("popToDestination", args.popToDestination)
            findParentNavController().navigate(R.id.postDraftFragment, mBundle)
            return
        }

        if (videoPath != null) {
            this.filePath = videoPath
        }

        if (videoContentUri != null) {
            this.filePath = videoContentUri
        }

        if (photoUri != null) {
            this.filePath = photoUri.toString()
        }
    }

    companion object {
        const val LANGUAGE_CLICK = 1
        const val PHOTO_CLICK = 2
        const val WIFI_CLICK = 3
        private var toolbar: Toolbar? = null

        fun showToolbar() {
            toolbar?.show()
        }
    }
}
