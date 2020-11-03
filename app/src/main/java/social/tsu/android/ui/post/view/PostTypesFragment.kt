package social.tsu.android.ui.post.view

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_post_types.*
import social.tsu.android.R
import social.tsu.android.ext.hide
import social.tsu.android.ext.show
import social.tsu.android.ui.post.helper.CameraHelper
import social.tsu.android.ui.post.helper.LayoutChooseHelper
import social.tsu.android.ui.post.helper.LayoutChooseHelper.Companion.changeLayoutAlpha
import social.tsu.android.ui.post.helper.LayoutChooseHelper.Companion.setChoose
import social.tsu.android.ui.post.model.FilterVideoModel
import social.tsu.android.ui.post.view.filter.FilterVideoAdapter
import social.tsu.android.ui.post.view.trim.features.trim.VideoTrimmerUtil
import social.tsu.android.ui.post.view.viewpager.*
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.camerarecorder.widget.Filters
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList


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
 * - postingType - {@link social.tsu.android.ui.new_post.PostDraftType} defaults to POST. May be POST, MESSAGE, COMMUNITY
 * - membership - used when posting to community, optional
 *
 * In order to add new source for media you need to add new fragment to post_types_graph.xml and
 * add menu item to post_bottom_menu.xml with android:id=<your_fragment_id_in_navgraph>
 * You can extend {@link BaseCameraFragment} which provides camera initialization routines, or
 * create completely custom fragment if you need more customization
 */

open class PostTypesFragment : Fragment(), Serializable {

    private val args: PostTypesFragmentArgs by navArgs()

    private var cameraHelper: CameraHelper? = null


    val allowVideo by lazy { args.allowVideo }

    // New post view pager fragments
    private val fragments: ArrayList<Fragment> by lazy {
        val fragments = ArrayList<Fragment>()
        fragments.add(PhotoCameraPostFragment())
        fragments.add(RecordVideoPostFragment())
        fragments.add(GifPostFragment())
        return@lazy fragments
    }

    var sharedViewModel: SharedViewModel? = null

    // Video record time
    private var mTimer: Timer? = null
    private var timer: TimerTask? = null
    private var seconds = 0L

    private var filePath: String? = null

    // Filters
    private var filterVideoAdapter: FilterVideoAdapter? = null
    private var selectedFilterItemPosition: Int = -1

    private fun recordingMode(recording: Boolean) {

        if (recording) {

            camera_rotate_id?.hide(animate = true)
            post_bottom_navbar?.hide(invisible = true, animate = true)
            (newPostViewPager as? TsuViewPager)?.enableSwiping(false)
            snap_icon_id.setImageResource(R.drawable.video_record_start)
            sectionsLayout?.hide(animate = true)
            optionsLayout?.hide(animate = true)
            gallery_image_id?.hide(animate = true)
        } else {

            snap_icon_id.setImageResource(R.drawable.record_video_not_start)
            (newPostViewPager as? TsuViewPager)?.enableSwiping(true)
            sectionsLayout?.show(animate = true)
            optionsLayout?.show(animate = true)
            gallery_image_id?.show(animate = true)
            camera_rotate_id?.show(animate = true)
            post_bottom_navbar?.show(animate = true)
        }

    }

    override fun onStop() {
        super.onStop()
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
        recordingMode(false)
        // Init view models
        initViewModels()
        // Ui initialization
        iniUi()
        // Init on click listeners
        initOnClicks()
        //init camera helper
        initCameraHelper()
    }

    private fun initCameraHelper() {
        cameraHelper = CameraHelper(requireActivity(), requireContext(), requireView())
    }

    private fun startRecordingTimer(start: Boolean, ignoreRecordingUi: Boolean = false) {

        // Check if timer is null
        if (timer == null) {
            timer = object : TimerTask() {
                override fun run() {
                    seconds++

                    val hours = seconds / 3600
                    val minutes = seconds % 3600 / 60
                    val seconds = seconds % 60
                    requireActivity().runOnUiThread {
                        timerText?.text = "$hours:$minutes:$seconds"
                    }
                }
            }
        }

        if (!start) {
            // Stop timer case
            timerText?.visibility = GONE
            timerText?.text = ""
            timer?.cancel();
            timer = null;
            seconds = 0L
            if (!ignoreRecordingUi) {
                recordingMode(false)
            }
            return
        }

        // Start timer case
        timerText?.text = ""
        timerText?.visibility = VISIBLE
        if (timer == null) {
            return
        }
        mTimer = Timer()
        mTimer?.scheduleAtFixedRate(timer, 0L, 1000L)
        if (!ignoreRecordingUi) {
            recordingMode(true)
        }
    }

    override fun onStart() {
        super.onStart()
        // Get view pager current page position
        val currentPagePosition = (newPostViewPager as TsuViewPager).currentItem
        // Handle view pager after changing
        LayoutChooseHelper.handleViewPagerChange(
            requireContext(),
            currentPagePosition,
            newPostPhotoText,
            newPostVideoText,
            newPostGifText,
            snap_icon_id,
            fragments
        )

        if ((newPostViewPager as TsuViewPager).currentItem == 0) {

            filterLayout_id.hide()
            handleFilterMode(true)
        }
    }


    private fun initOnClicks() {

        // listen bottom buttons click listeners
        onClickButtons()

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

        // Listen filter close layout
        filterCancel?.setOnClickListener {

            handleFilterMode(true)
        }

        // Listen apply filter
        filterDone?.setOnClickListener {

            handleFilterMode(applyFilter = true)
        }
    }

    private fun handleStartCamera() {

        // Get view pager current page position
        when ((newPostViewPager as TsuViewPager).currentItem) {
            0 -> {
                val fragment = fragments[0] as PhotoCameraPostFragment

                handlePhotoCaptureClicked(fragment)
            }
            1 -> {
                val fragment = fragments[1] as RecordVideoPostFragment

                handleVideoRecordClicked(fragment)
            }
            else -> {

                val fragment = fragments[2] as GifPostFragment

                handleRecordGifClicked(fragment)
            }
        }
    }

    private fun handleRecordGifClicked(fragment: GifPostFragment) {

        if (isTimerRunning()) {
            startRecordingTimer(false, ignoreRecordingUi = true)
            fragment.stopRecording {
                this.filePath = it

                if (this.filePath == null) {
                    Toast.makeText(
                        requireContext(),
                        "Can not continue, file is empty.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@stopRecording
                }

                val mBundle = Bundle()
                mBundle.putString("filePath", filePath)
                mBundle.putInt("fromScreenType", getScreenType())
                mBundle.putSerializable("postTypeFragment", this)
                sharedViewModel!!.select(false)
                // Configure trimmer video limitation
                VideoTrimmerUtil.TYPE = 2
                Handler(Looper.getMainLooper()).postDelayed({
                    findParentNavController().navigate(R.id.postTrimFragment, mBundle)
                }, 200)
            }
            return
        }

        fragment.recordGif { onCancel: Boolean, onStart: Boolean ->

            if (onCancel) {
                this.filePath = null
                startRecordingTimer(false)
                return@recordGif
            }

            if (onStart) {
                this.filePath = null
                startRecordingTimer(true)
                return@recordGif
            }
        }
    }

    private fun handleVideoRecordClicked(fragment: RecordVideoPostFragment) {

        if (isTimerRunning()) {
            startRecordingTimer(false, ignoreRecordingUi = true)
            fragment.stopRecording {
                this.filePath = it

                if (this.filePath == null) {
                    Toast.makeText(
                        requireContext(),
                        "Can not continue, file is empty.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@stopRecording
                }

                val mBundle = Bundle()
                mBundle.putString("filePath", filePath)
                mBundle.putInt("fromScreenType", getScreenType())
                mBundle.putSerializable("postTypeFragment", this)
                sharedViewModel!!.select(false)
                // Configure trimmer video limitation
                VideoTrimmerUtil.TYPE = 1
                Handler(Looper.getMainLooper()).postDelayed({
                    findParentNavController().navigate(R.id.postTrimFragment, mBundle)
                }, 200)
            }
            return
        }

        fragment.recordVideo { onCancel: Boolean, onStart: Boolean ->

            if (onCancel) {
                this.filePath = null
                startRecordingTimer(false)
                return@recordVideo
            }

            if (onStart) {
                this.filePath = null
                startRecordingTimer(true)
                return@recordVideo
            }
        }
    }

    private fun handlePhotoCaptureClicked(fragment: PhotoCameraPostFragment) {

        fragment.capturePicture {
            this.filePath = it

            if (this.filePath == null) {
                Toast.makeText(
                    requireContext(),
                    "Can not continue, file is empty.",
                    Toast.LENGTH_LONG
                ).show()
                return@capturePicture
            }

            val mBundle = Bundle()
            mBundle.putString("filePath", filePath)
            mBundle.putInt("fromScreenType", getScreenType())
            mBundle.putSerializable("postTypeFragment", this)
            sharedViewModel!!.select(false)
            findParentNavController().navigate(R.id.postPreviewFragment, mBundle)
        }
    }

    private fun isTimerRunning(): Boolean {
        return timerText?.visibility == VISIBLE
    }

    private fun handleSwitchCamera() {

        // Get view pager current page position
        when ((newPostViewPager as TsuViewPager).currentItem) {
            0 -> {
                (fragments[0] as PhotoCameraPostFragment).switchCamera()
            }
            1 -> {
                (fragments[1] as RecordVideoPostFragment).switchCamera()
            }
            else -> {
                (fragments[2] as GifPostFragment).switchCamera()
            }
        }
    }

    private fun getScreenType(): Int {

        return (newPostViewPager as TsuViewPager).currentItem
    }

    private fun iniUi() {

        // Init new post view pager
        initNewPostViewPagerAdapter()
        // Set default Camera Mode
        setChoose(
            PHOTO_CLICK,
            view?.findViewById(R.id.languageLayout_id)!!,
            view?.findViewById(R.id.photoLayout_id)!!,
            view?.findViewById(R.id.wifiLayout_id)!!
        )
        // Hide tool bar
        post_types_toolbar.visibility = View.GONE
    }

    private fun initViewModels() {
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    private fun initNewPostViewPagerAdapter() {

        val newPostAdapter = NewPostViewPager(childFragmentManager, fragments.size, fragments)
        (newPostViewPager as TsuViewPager).setPageTransformer(true, ZoomOutPageTransformer())
        (newPostViewPager as TsuViewPager).offscreenPageLimit = 3
        (newPostViewPager as TsuViewPager).adapter = newPostAdapter
        (newPostViewPager as TsuViewPager).addOnPageChangeListener(object :
            ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                // Handle view pager after changing
                LayoutChooseHelper.handleViewPagerChange(
                    requireContext(),
                    position,
                    newPostPhotoText,
                    newPostVideoText,
                    newPostGifText,
                    snap_icon_id,
                    fragments
                )

                startRecordingTimer(false, ignoreRecordingUi = true)

                if ((newPostViewPager as TsuViewPager).currentItem == 0) {

                    filterLayout_id.hide()
                    handleFilterMode(true)

                    if (isFilterPending()) {
                        // Gone filter list layout
                        filterListLayout?.hide()
                    }
                } else {
                    filterLayout_id.show()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    private fun onClickButtons() {

        view?.findViewById<ConstraintLayout>(R.id.flashLayout_id)?.setOnClickListener {

            // Get view pager current page position
            when ((newPostViewPager as TsuViewPager).currentItem) {
                0 -> {
                    (fragments[0] as PhotoCameraPostFragment).handleFlash()
                }
                1 -> {
                    (fragments[1] as RecordVideoPostFragment).handleFlash()
                }
                else -> {
                    (fragments[2] as GifPostFragment).handleFlash()
                }
            }

            // Changing layout alpha after clicking
            changeLayoutAlpha(flashLayout_id)
        }

        view?.findViewById<ConstraintLayout>(R.id.soundLayout_id)?.setOnClickListener {
            // Changing layout alpha after clicking
            changeLayoutAlpha(soundLayout_id)
        }

        view?.findViewById<ConstraintLayout>(R.id.languageLayout_id)?.setOnClickListener {
            setChoose(
                LANGUAGE_CLICK,
                view?.findViewById(R.id.languageLayout_id),
                view?.findViewById(R.id.photoLayout_id),
                view?.findViewById(R.id.wifiLayout_id)
            )
        }

        view?.findViewById<ConstraintLayout>(R.id.photoLayout_id)?.setOnClickListener {
            setChoose(
                PHOTO_CLICK,
                view?.findViewById(R.id.languageLayout_id),
                view?.findViewById(R.id.photoLayout_id),
                view?.findViewById(R.id.wifiLayout_id)
            )
        }

        view?.findViewById<ConstraintLayout>(R.id.wifiLayout_id)?.setOnClickListener {
            setChoose(
                WIFI_CLICK,
                view?.findViewById(R.id.languageLayout_id),
                view?.findViewById(R.id.photoLayout_id),
                view?.findViewById(R.id.wifiLayout_id)
            )
        }

        view?.findViewById<ConstraintLayout>(R.id.closeLayout_id)?.setOnClickListener {
            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.mainFeedFragment, false)
        }

        view?.findViewById<ConstraintLayout>(R.id.filterLayout_id)?.setOnClickListener {

            // Handle filter clicked
            handleFilterMode()
        }
    }

    private fun handleFilterMode(hideFilter: Boolean = false, applyFilter: Boolean = false) {

        selectedFilterItemPosition = -1
        val pagePosition = (newPostViewPager as TsuViewPager).currentItem

        if (pagePosition == 0) return

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
                changeCameraFilter((filterVideoAdapter?.getData()?.get(0)?.filterObject as Filters))
            }
        }
    }

    private fun initFilterAdapter() {

        filterRecyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        filterVideoAdapter = FilterVideoAdapter()

        val filters = Filters.values()
        val filterItems = arrayListOf<FilterVideoModel>()
        filters.forEach {
            filterItems.add(FilterVideoModel(R.drawable.cover, it))
        }
        filterVideoAdapter?.addItemClickListener { position: Int, itemModel: FilterVideoModel ->

            // Handle filter item clicked
            filterNameLayout?.show(animate = true, duration = 500)
            handleFilterItemClicked(position, itemModel)
        }
        filterVideoAdapter?.submitList(filterItems)
        filterRecyclerView?.adapter = filterVideoAdapter
    }

    private fun handleFilterItemClicked(position: Int, itemModel: FilterVideoModel) {

        val currentItems = filterVideoAdapter?.getData() ?: return

        if (selectedFilterItemPosition > -1) {
            currentItems[selectedFilterItemPosition].select(false)
            filterVideoAdapter?.notifyItemChanged(selectedFilterItemPosition)
        }
        currentItems[position].select(true)
        filterVideoAdapter?.notifyItemChanged(position)

        selectedFilterItemPosition = position

        filterNameBuble?.text = (itemModel.filterObject as Filters).value
        changeCameraFilter((itemModel.filterObject as Filters))
    }

    private fun isFilterPending(): Boolean = filterListLayout?.visibility == VISIBLE

    private fun changeCameraFilter(filters: Filters) {

        return when ((newPostViewPager as TsuViewPager).currentItem) {
            1 -> {
                (fragments[1] as RecordVideoPostFragment).handleFilter(filters)
            }
            else -> {
                (fragments[2] as GifPostFragment).handleFilter(filters)
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
        fromGrid: Boolean? = null
    ) {

        if (fromGrid == true) {

            val mBundle = Bundle()
            mBundle.putString("videoPath", videoPath)
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
        private const val STATE_SELECTED_ID = "id"
        const val LANGUAGE_CLICK = 1
        const val PHOTO_CLICK = 2
        const val WIFI_CLICK = 3
        private var toolbar: Toolbar? = null

        fun showToolbar() {
            toolbar?.show()
        }
    }
}

sealed class RecordingState {
    object Recording : RecordingState()
    object Stopped : RecordingState()
    object Canceled : RecordingState()
}
