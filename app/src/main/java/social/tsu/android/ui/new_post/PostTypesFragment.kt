package social.tsu.android.ui.new_post

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_post_types.*
import social.tsu.android.R
import social.tsu.android.helper.navigateSafe
import social.tsu.android.utils.findParentNavController
import social.tsu.android.utils.setScreenOrientation
import social.tsu.android.utils.show
import social.tsu.android.viewModel.SharedViewModel
import java.lang.Exception

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

    private val navController by lazy {
        (childFragmentManager
            .findFragmentById(R.id.post_types_nav_host_fragment) as NavHostFragment).navController
    }

    var sharedViewModel: SharedViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        return inflater.inflate(R.layout.fragment_post_types, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //setup toolbar with nested nav graph
        post_types_toolbar.visibility = View.GONE
//        post_types_toolbar.setupWithNavController(navController)
//
//        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
//
//        post_types_toolbar.menu.findItem(R.id.action_cancel).setOnMenuItemClickListener {
//            post_types_toolbar.visibility = View.VISIBLE
//            if (args.postingType == PostDraftType.MESSAGE) {
//                findParentNavController().popBackStack(R.id.chatFragment, false)
//            } else if (args.postingType == PostDraftType.COMMUNITY) {
//                findParentNavController().popBackStack(R.id.postDraftFragment, false)
//            } else {
//                sharedViewModel!!.select(false)
//                findParentNavController().popBackStack(R.id.mainFeedFragment, false)
//            }
//            return@setOnMenuItemClickListener true
//        }
//
//
//        toolbar = post_types_toolbar

        onClickButtons()

        //restore fragment state if any
        savedInstanceState?.getInt(STATE_SELECTED_ID)?.let {
            navController.navigate(it)
        }
    }

    private fun onClickButtons() {
        view?.findViewById<ConstraintLayout>(R.id.languageLayout_id)?.setOnClickListener {
            setChoose(LANGUAGE_CLICK)
            setUnChoose(PHOTO_CLICK)
            setUnChoose(WIFI_CLICK)
        }

        view?.findViewById<ConstraintLayout>(R.id.photoLayout_id)?.setOnClickListener {
            setChoose(PHOTO_CLICK)
            setUnChoose(LANGUAGE_CLICK)
            setUnChoose(WIFI_CLICK)
        }

        view?.findViewById<ConstraintLayout>(R.id.wifiLayout_id)?.setOnClickListener {
            setChoose(WIFI_CLICK)
            setUnChoose(PHOTO_CLICK)
            setUnChoose(LANGUAGE_CLICK)
        }

        view?.findViewById<ConstraintLayout>(R.id.libraryLayout_id)?.setOnClickListener {
            findNavController().navigate(R.id.mediaLibraryFragment)
        }

        view?.findViewById<ConstraintLayout>(R.id.closeLayout_id)?.setOnClickListener {
            findParentNavController().popBackStack(R.id.mainFeedFragment, false)
        }
    }

    private fun setChoose(layout: Int) {
        when (layout) {
            LANGUAGE_CLICK -> {
               view?.findViewById<ConstraintLayout>(R.id.languageLayout_id)
                   ?.setBackgroundResource(R.drawable.ic_languages_white_end)
            }

            PHOTO_CLICK -> {
                view?.findViewById<ConstraintLayout>(R.id.photoLayout_id)
                    ?.setBackgroundResource(R.drawable.ic_photo_white)
            }

            WIFI_CLICK -> {
                view?.findViewById<ConstraintLayout>(R.id.wifiLayout_id)
                    ?.setBackgroundResource(R.drawable.ic_wifi_white_finish)
            }
        }
    }

    private fun setUnChoose(layout: Int) {
        when (layout) {
            LANGUAGE_CLICK -> {
                view?.findViewById<ConstraintLayout>(R.id.languageLayout_id)
                    ?.setBackgroundResource(R.drawable.ic_languages_gray)
            }

            PHOTO_CLICK -> {
                view?.findViewById<ConstraintLayout>(R.id.photoLayout_id)
                    ?.setBackgroundResource(R.drawable.ic_photogray)
            }

            WIFI_CLICK -> {
                view?.findViewById<ConstraintLayout>(R.id.wifiLayout_id)
                    ?.setBackgroundResource(R.drawable.ic_wifi_gray_finish)
            }
        }
    }


    override fun onDestroyView() {
        setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        post_types_toolbar.visibility = View.VISIBLE
        super.onDestroyView()
        sharedViewModel!!.select(false)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            sharedViewModel!!.select(false)
        } catch (e:Exception){
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

    companion object {
        private const val STATE_SELECTED_ID = "id"
        private const val LANGUAGE_CLICK = 1
        private const val PHOTO_CLICK = 2
        private const val WIFI_CLICK = 3
        private var toolbar: Toolbar? = null

        fun showToolbar() {
            toolbar?.show()
        }
    }
}
