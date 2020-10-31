package social.tsu.android.ui.post.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_post_types.*
import social.tsu.android.R
import social.tsu.android.helper.DeviceFlashHelper
import social.tsu.android.helper.navigateSafe
import social.tsu.android.ui.post.helper.LayoutChooseHelper
import social.tsu.android.ui.post.helper.LayoutChooseHelper.Companion.changeLayoutAlpha
import social.tsu.android.ui.post.helper.LayoutChooseHelper.Companion.setChoose
import social.tsu.android.ui.post.view.viewpager.*
import social.tsu.android.utils.findParentNavController
import social.tsu.android.utils.show
import social.tsu.android.viewModel.SharedViewModel


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

class PostTypesFragment : Fragment() {

    private val args: PostTypesFragmentArgs by navArgs()

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

    override fun onStart() {
        super.onStart()
        DeviceFlashHelper.registerFlashlightState(requireContext())
    }

    override fun onStop() {
        super.onStop()
        // Disable Flash
        DeviceFlashHelper.switchFlashLight(false)
        DeviceFlashHelper.unregisterFlashlightState(requireContext())
    }

    private fun initOnClicks() {

        // listen bottom buttons click listeners
        onClickButtons()
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

        //handle view pager after changing
        LayoutChooseHelper.handleViewPagerChange(
            requireContext(),
            0,
            newPostPhotoText,
            newPostVideoText,
            newPostGifText,
            fragments
        )
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
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    private fun onClickButtons() {

        view?.findViewById<ConstraintLayout>(R.id.flashLayout_id)?.setOnClickListener {

            if (!DeviceFlashHelper.deviceFlashIsAvailable()) {
                showNoFlashError()
                return@setOnClickListener
            }

            val flashIsOn = DeviceFlashHelper.isFlashlightOn

            DeviceFlashHelper.switchFlashLight(!flashIsOn)

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

        view?.findViewById<ConstraintLayout>(R.id.mediaLibraryLayout_id)?.setOnClickListener {
            findParentNavController().navigate(R.id.mediaLibraryLayout_id)
        }

        view?.findViewById<ConstraintLayout>(R.id.closeLayout_id)?.setOnClickListener {
            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.mainFeedFragment, false)
        }
    }

    override fun onDestroyView() {
        //setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        post_types_toolbar.visibility = View.VISIBLE
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
        photoUri: Uri? = null
    ) {
        val direction = PostTypesFragmentDirections.next(
            videoPath, videoContentUri, photoUri
        ).apply {
            this.postText = args.postText
            this.recipient = args.recipient
            this.postingType = args.postingType
            this.membership = args.membership
            this.allowVideo = args.allowVideo
            this.popToDestination = args.popToDestination
        }

        val navOptions = NavOptions.Builder()
            .setPopUpTo(args.popToDestination, false)
            .build()

        findParentNavController().navigateSafe(direction, navOptions)
    }

    private fun showNoFlashError() {

        Toast.makeText(
            requireContext(),
            "Flash not available in this device...",
            Toast.LENGTH_LONG
        ).show()
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
