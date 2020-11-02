package social.tsu.android.ui.post.view

import android.app.AlertDialog
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
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_post_types.*
import social.tsu.android.R
import social.tsu.android.ui.post.helper.Filters
import social.tsu.android.ui.post.helper.LayoutChooseHelper
import social.tsu.android.ui.post.helper.LayoutChooseHelper.Companion.changeLayoutAlpha
import social.tsu.android.ui.post.helper.LayoutChooseHelper.Companion.setChoose
import social.tsu.android.ui.post.view.viewpager.*
import social.tsu.android.utils.findParentNavController
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.camerarecorder.CameraRecorder
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

class PostTypesFragment : Fragment(), Serializable {

    private val args: PostTypesFragmentArgs by navArgs()

    //filter dialog field
    private var filterDialog: AlertDialog? = null

    protected var cameraRecorder: CameraRecorder? = null


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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        return inflater.inflate(R.layout.fragment_post_types, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init view models
        initViewModels()
        // Ui initialization
        iniUi()
        // Init on click listeners
        initOnClicks()
    }

    private fun startRecordingTimer(start: Boolean) {

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
    }

    override fun onStart() {
        super.onStart()
        // Get view pager current page position
        val currentPagePosition = newPostViewPager.currentItem
        // Handle view pager after changing
        LayoutChooseHelper.handleViewPagerChange(
            requireContext(),
            currentPagePosition,
            newPostPhotoText,
            newPostVideoText,
            newPostGifText,
            fragments
        )
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
    }

    private fun handleStartCamera() {

        // Get view pager current page position
        when (newPostViewPager.currentItem) {
            0 -> {
                val fragment = fragments[0] as PhotoCameraPostFragment

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
            1 -> {
                val fragment = fragments[1] as RecordVideoPostFragment

                if (isTimerRunning()) {
                    startRecordingTimer(false)
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
            else -> {

                val fragment = fragments[2] as GifPostFragment

                if (isTimerRunning()) {
                    startRecordingTimer(false)
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
        }
    }

    private fun isTimerRunning(): Boolean {
        return timerText?.visibility == VISIBLE
    }

    private fun handleSwitchCamera() {

        // Get view pager current page position
        when (newPostViewPager.currentItem) {
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

        return newPostViewPager.currentItem
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
        newPostViewPager.setPageTransformer(true, ZoomOutPageTransformer())
        newPostViewPager.offscreenPageLimit = 3
        newPostViewPager.adapter = newPostAdapter
        newPostViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
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
                    fragments
                )

                startRecordingTimer(false)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    private fun onClickButtons() {

        view?.findViewById<ConstraintLayout>(R.id.flashLayout_id)?.setOnClickListener {

            // Get view pager current page position
            when (newPostViewPager.currentItem) {
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
            if (filterDialog == null) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Choose a filter")
                builder.setOnDismissListener { dialog ->
                    filterDialog = null
                }
                val filters = Filters.values()
                val charList =
                    arrayOfNulls<CharSequence>(filters.size)
                var i = 0
                val n = filters.size
                while (i < n) {
                    charList[i] = filters[i].name
                    i++
                }
                builder.setItems(charList) { dialog, item ->
                    changeFilter(filters[item])
                }
                filterDialog = builder.show()
            } else {
                filterDialog!!.dismiss()
            }
        }
    }

    private fun changeFilter(filters: Filters) {
        cameraRecorder?.setFilter(Filters.getFilterInstance(filters, requireContext()))
    }

    override fun onDestroyView() {
        //setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
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

        fun hideToolbar() {
            toolbar?.hide()
        }
    }
}

sealed class RecordingState {
    object Recording : RecordingState()
    object Stopped : RecordingState()
    object Canceled : RecordingState()
}
