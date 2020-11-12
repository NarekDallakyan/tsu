package social.tsu.android.ui.post.view.draft

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.IdRes
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import coil.api.load
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.draft_post.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.entity.FeedSource
import social.tsu.android.data.local.entity.Post
import social.tsu.android.data.local.entity.PostPayload
import social.tsu.android.data.local.models.PostUser
import social.tsu.android.execute
import social.tsu.android.ext.getGifDrawable
import social.tsu.android.ext.getVideoThumbnail
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.api.PostImageApi
import social.tsu.android.network.api.StreamApi
import social.tsu.android.network.model.*
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.*
import social.tsu.android.ui.CameraUtil
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.checkPermissions
import social.tsu.android.ui.messaging.chats.ChatFragmentArgs
import social.tsu.android.ui.post.helper.PostDraftVisibilityUiHelper
import social.tsu.android.ui.post.helper.PostTypeDraftUiHelper
import social.tsu.android.ui.post.view.draft.PostDraftFragmentDirections.showPostTypesFragment
import social.tsu.android.ui.post_feed.community.CommunityFeedFragmentDirections
import social.tsu.android.utils.*
import social.tsu.android.viewModel.MentionViewModel
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.android.workmanager.workers.UploadVideoWorker
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class PostDraftType : Serializable {
    POST, MESSAGE, COMMUNITY
}

private const val TAG = "PostDraftFragment"

/**
 * Fragment that represents post creation UI
 * Provides text field, privacy controls, media preview and buttons for attaching videos and images
 *
 * Used when user already selected any media or if he wants to edit existing post.
 * Typically flow starts from top of the feed (special feed item that allow user to type post text in the feed itself)
 * or by tapping (+) button on the bottom navigation bar. PostTypesFragment shows up, and after user selects media it
 * passes data to this PostDraftFragment.
 */
class PostDraftFragment : Fragment() {

    private lateinit var dateFormat: SimpleDateFormat
    private var dspTimer: Disposable? = null
    private lateinit var bitmap: Bitmap
    private lateinit var gifDrawable: GifDrawable
    private var imageType: String? = null

    @Inject
    lateinit var postApi: PostApi

    @Inject
    lateinit var postImageApi: PostImageApi

    @Inject
    lateinit var postFeedDao: PostFeedDao

    @Inject
    lateinit var communityApi: CommunityApi

    @Inject
    lateinit var chatService: ChatService

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var streamApi: StreamApi

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    private var sharedViewModel: SharedViewModel? = null


    val properties = HashMap<String, Any?>()

    @IdRes
    var defaultPostType: Int = R.id.photoCaptureFragment2
    private var compositeDisposable = CompositeDisposable()
    private var lastSnackbar: Snackbar? = null
    private var mentionViewModel: MentionViewModel? = null

    // Ui handler
    private val draftUiHandler = PostTypeDraftUiHelper
    private val visibilityUiHandler = PostDraftVisibilityUiHelper


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dateFormat = SimpleDateFormat("yyyy-MM-dd")
        startTimer()
        // init OnClicks
        initOnClicks()
        // Add Content
        initUi()
    }

    override fun onDestroyView() {
        compositeDisposable.dispose()
        dspTimer?.dispose()
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        val view = inflater.inflate(R.layout.draft_post, container, false)
        // Get argument data
        getArgumentData()
        // init view model
        initViewModel()
        setHasOptionsMenu(true)
        return view
    }

    private fun initViewModel() {

        mentionViewModel = ViewModelProvider(requireActivity()).get(MentionViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    private fun initOnClicks() {

        // Listen close layout clicked
        closePostDraft?.setOnClickListener {
            sharedViewModel?.select(false)
            findParentNavController().navigate(R.id.postTypesFragment)
        }

        // Listen visibility layout clicked
        postVisibility?.setOnClickListener {
            sharedViewModel?.select(false)
            findParentNavController().navigate(R.id.postVisibilityFragment)
        }

        // Listen post button
        postButton?.setOnClickListener {

            // show progress bar and waiting for complete
            progress_bar.show()
            // handle save to device
            handleSaveOnDevice()
            // Waiting 1 second
            Handler().postDelayed({
                // handle post
                handlePost()
            }, 1000)
        }

        // Listen save on device item clicked
        saveDeviceLayout?.setOnClickListener {

            val isChecked = saveToDeviceSwitcher.isChecked
            saveToDeviceSwitcher.isChecked = !isChecked
        }
    }

    private fun handleSaveOnDevice() {

        if (photoUri == null) {
            val originalFilePath = originalFilePath ?: ""
            val originalFile = File(originalFilePath)
            if (originalFile.exists()) {
                val isDeleted = originalFile.delete()
            }
        }

        if (saveToDeviceSwitcher.isChecked) return

        val deletingUri = (when {
            photoUri != null -> {
                Uri.parse(photoUri)
            }
            videoPath != null -> {
                Uri.parse(videoPath)
            }
            videoContentUri != null -> {
                Uri.parse(videoContentUri)
            }
            else -> {
                null
            }
        })
            ?: return

        val filePath = deletingUri.path ?: return
        val deletingFile = File(filePath)
        if (deletingFile.exists()) {
            val isDeleted = deletingFile.delete()
        }
    }

    // Argument properties
    private var videoPath: String? = null
    private var videoContentUri: String? = null
    private var photoUri: String? = null
    private var postText: String? = null
    private var postUser: PostUser? = null
    private var postDraftType: PostDraftType? = null
    private var membership: Membership? = null
    private var allowVideo: Boolean? = null
    private var popToDestination: Int? = null
    private var originalFilePath: String? = null

    private fun getArgumentData() {

        if (arguments == null) return

        videoPath = requireArguments().getString("videoPath")
        originalFilePath = requireArguments().getString("originalFilePath")
        videoContentUri = requireArguments().getString("videoContentUri")
        photoUri = requireArguments().getString("photoUri")
        postText = requireArguments().getString("postText")
        postUser = requireArguments().getParcelable("recipient")
        postDraftType = requireArguments().getSerializable("postingType") as? PostDraftType?
        membership = requireArguments().getParcelable("membership")
        allowVideo = requireArguments().getBoolean("allowVideo")
        popToDestination = requireArguments().getInt("popToDestination")
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable = CompositeDisposable()
        mentionViewModel?.selectTag("")
    }

    override fun onDestroy() {
        super.onDestroy()
        mentionViewModel?.selectTag("")
    }

    private fun initUi() {

        // handle description close visibility
        draftUiHandler.handleDescriptionUi(view)
        // handle save to device ui
        draftUiHandler.handleSaveToDeviceUi(view)
        // handle visibility ui
        visibilityUiHandler.showChoosesOption(view)
    }

    override fun onStart() {
        super.onStart()

        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()

        photoUri?.let {
            try {
                imageType = context?.contentResolver?.getType(Uri.parse(photoUri)) ?: ""
                handlePhoto(Uri.parse(it))
            } catch (e: Exception) {
                snack(getString(R.string.unable_to_load_attached_photo))
            }
            return
        }

        videoPath?.let {
            imageType = context?.contentResolver?.getType(Uri.parse(videoPath)) ?: ""
            handleVideo(it)
            return
        }
        videoContentUri?.let {
            imageType = context?.contentResolver?.getType(Uri.parse(videoContentUri)) ?: ""
            handleVideoContentPath(it)
            return
        }
    }

    override fun onStop() {
        super.onStop()
        dismissKeyboard()
    }

    /**
     * Make a post with video
     *
     *  Unlike images, videos are large and are treated differently. They need to be uploaded as Streams
     *  with following algo:
     *  - create steam on server using StreamApi
     *  - upload video to server using id of a stream obtained on previous step
     *  - make a post using stream id
     *
     * First of all if video is too large it get's compressed so it will be accepted by server and does not
     * exceed max file size limit of 50 MB
     * Then it is passed to {@link UploadVideoWorker} which uploads video to our server in background
     * and after successful upload makes an actual post
     */
    private fun postVideo(videoPath: String, message: String) {
        val privacy = when (visibilityUiHandler.getChoosesOption()) {
            1 -> {
                Post.PRIVACY_PRIVATE
            }
            0 -> {
                Post.PRIVACY_PUBLIC
            }
            2 -> {
                Post.PRIVACY_EXCLUSIVE
            }
            else -> {
                Post.PRIVACY_PUBLIC
            }
        }
        UploadVideoWorker.start(
            videoPath,
            message,
            membership?.group?.id ?: -1,
            privacy = privacy
        )
        if (privacy == Post.PRIVACY_EXCLUSIVE)
            setTime()
        popBackStack()
    }

    private fun setTime() {
        sharedPrefManager.setExclusivePostTime(dateFormat.format(System.currentTimeMillis()))
    }

    /**
     * Same as {@link #postVideo(videoPath: String, message: String)} but handles Uri and obtains
     * actual path to needed file from ContentResolver
     */
    private fun postVideoPath(videoPath: Uri, message: String) {

        val privacy = when (visibilityUiHandler.getChoosesOption()) {
            1 -> {
                Post.PRIVACY_PRIVATE
            }
            0 -> {
                Post.PRIVACY_PUBLIC
            }
            2 -> {
                Post.PRIVACY_EXCLUSIVE
            }
            else -> {
                Post.PRIVACY_PUBLIC
            }
        }

        UploadVideoWorker.start(
            videoPath,
            message,
            membership?.group?.id ?: -1,
            privacy = privacy
        )
        if (privacy == Post.PRIVACY_EXCLUSIVE)
            setTime()
        popBackStack()
    }

    /**
     * Sets up preview from video file.
     */
    private fun handleVideo(videoPath: String) {
        try {

            if (videoPath.contains("gif")) {
                val gifDrawable = videoPath.getGifDrawable()
                imgPreview.setImageDrawable(gifDrawable)
            } else {
                val videoThumbnail = videoPath.getVideoThumbnail()
                videoThumbnail?.let {
                    imgPreview?.load(it)
                }
            }

        } catch (e: java.lang.Exception) {
            Log.e("IMAGE", "error reading stream", e)
        }
    }

    fun openPostTypesFragment() {
        addMediaToPost(defaultPostType)
    }

    /**
     * Opens PostTypesFragment with specific source type preselected
     *
     * @param mediaType id of a fragment that should be preselected when media capture
     * interface opens
     */
    private fun addMediaToPost(mediaType: Int) {

        defaultPostType = mediaType
        checkPermissions(
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ),
            MainActivity.NEW_POST_FROM_FEED_PERMISSIONS_REQUEST_CODE
        ) {
            descriptionEditText?.let {
                //Filling in all arguments for PostTypesFragment:
                //setting defaultPostType (which tab should be selected on launch)
                //and copying items from current fragment's arguments to pass them forward to PostTypesFragment
                val text = it.text.toString()
                val action = showPostTypesFragment(text).apply {
                    defaultPostType = mediaType
                    postingType = postDraftType ?: PostDraftType.POST
                    membership = membership
                    recipient = postUser
                    allowVideo = allowVideo
                    popToDestination = popToDestination
                }
                findNavController().navigate(action)
            }
        }
    }


    /**
     * Setup video preview from provided Uri
     */
    private fun handleVideoContentPath(path: String) {

        try {
            if (path.contains("gif")) {
                val gifDrawable = path.getGifDrawable()
                imgPreview.setImageDrawable(gifDrawable)
            } else {
                val videoThumbnail = path.getVideoThumbnail()
                videoThumbnail?.let {
                    imgPreview?.load(it)
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e("IMAGE", "error reading stream", e)
        }
    }


    private fun handlePhoto(photoUri: Uri) {
        lifecycleScope.launch {

            whenStarted {

                imageType = context?.contentResolver?.getType(photoUri) ?: ""

                withContext(Dispatchers.IO) {
                    if (photoUri.scheme == null) {
                        // camera use case
                        val testFile = File(photoUri.toString())
                        processBitmap(compressFile(testFile), ExifInterface(testFile), false)
                    } else {
                        // gallery use case
                        val context = activity ?: return@withContext
                        var file: File? = null
                        try {
                            Glide.with(context)
                                .asFile()
                                .load(photoUri)
                                .submit()
                                .get()
                        } catch (e: java.lang.Exception) {

                        }

                        if (file == null) {
                            try {
                                file = CameraUtil.getBitmapFromContent(context, photoUri) ?: run {
                                    Log.w("PostDataFragment", "Can't load file")
                                    return@withContext
                                }
                            } catch (e: SecurityException) {
                                analyticsHelper.log("SecurityException for $photoUri")
                                Log.w("PostDataFragment", "SecurityException for $photoUri")
                                return@withContext
                            }
                        }

                        try {
                            if (imageType == "image/gif") {
                                processGif(getGifFile(file))
                            } else {
                                processBitmap(compressFile(file), ExifInterface(file), false)
                            }
                            file.delete()

                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun compressFile(imageFile: File): Bitmap {
        return if (imageFile.length() > MAX_IMAGE_SIZE) {
            val image = Glide.with(requireContext())
                .asBitmap()
                .load(imageFile)
                .submit()
                .get()
            val newHeight: Int
            val newWidth: Int
            if (image.height > image.width) {
                newHeight = MAX_RESOLUTION
                newWidth = ((image.width.toDouble() / image.height) * MAX_RESOLUTION).toInt()
            } else {
                newWidth = MAX_RESOLUTION
                newHeight = ((image.height.toDouble() / image.width) * MAX_RESOLUTION).toInt()
            }
            ImageUtils.getCompressedBitmap(
                imageFile.absolutePath,
                newHeight.toFloat(),
                newWidth.toFloat()
            )
        } else {
            Glide.with(requireContext())
                .asBitmap()
                .load(imageFile)
                .override(1024, 1024)
                .submit()
                .get()
        }
    }

    private fun getGifFile(file: File): GifDrawable {
        return Glide
            .with(requireContext())
            .asGif()
            .load(file)
            .submit()
            .get()
    }

    private suspend fun processGif(gif: GifDrawable) {
        gifDrawable = gif

        withContext(Dispatchers.Main) {
            Glide.with(requireContext())
                .load(gifDrawable)
                .into(imgPreview)
        }
    }

    private suspend fun processBitmap(
        sourceBitmap: Bitmap,
        exif: ExifInterface,
        applyOrientationChanges: Boolean = true
    ) {

        val orientation: Int = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        if (!applyOrientationChanges) {
            bitmap = sourceBitmap
        } else {
            bitmap = withContext(Dispatchers.IO) {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                        flipImage(sourceBitmap)
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        val flipped = flipImage(sourceBitmap)
                        rotateImg(flipped, 90f)
                    }
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        val flipped = flipImage(sourceBitmap)
                        rotateImg(flipped, 270f)
                    }
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateImg(
                        sourceBitmap,
                        90f
                    )
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateImg(
                        sourceBitmap,
                        180f
                    )
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateImg(
                        sourceBitmap,
                        270f
                    )
                    else -> sourceBitmap
                }
            }
        }
        withContext(Dispatchers.Main) {

            imgPreview.setImageBitmap(bitmap)
        }
    }

    private fun flipImage(img: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postScale(-1f, 1f, (img.width / 2).toFloat(), (img.height / 2).toFloat())
        val flippedImg =
            Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return flippedImg
    }


    fun base64Encoder(image: ByteArray): String {
        return Base64.encodeToString(image, Base64.DEFAULT)
    }

    /**
     * Determine what type of posting (message, community, post) we have and post with image or gif
     * if any of them was added. Videos are handled in different way, see {@link #postVideo(String, String)}
     */
    private fun proceed() {
        progress_bar?.visibility = View.VISIBLE
        val text = descriptionEditText?.text.toString()
        val imageData = when (imageType) {
            null -> null
            "image/gif" -> {
                val byteBuffer = gifDrawable.buffer
                val bytes = ByteArray(byteBuffer.capacity())
                (byteBuffer.duplicate().clear() as ByteBuffer).get(bytes)
                "data:image/gif;base64," + base64Encoder(bytes)
            }
            else -> {
                val bytes = ByteArrayOutputStream()
                bitmap.compress(JPEG, 99, bytes)
                "data:image/jpeg;base64," + base64Encoder(bytes.toByteArray())
            }
        }

        when (postDraftType) {
            PostDraftType.MESSAGE -> sendMessage(text, imageData)
            else -> {
                val privacy =
                    when (visibilityUiHandler.getChoosesOption()) {
                        1 -> {
                            Post.PRIVACY_PRIVATE
                        }
                        0 -> {
                            Post.PRIVACY_PUBLIC
                        }
                        2 -> {
                            Post.PRIVACY_EXCLUSIVE
                        }
                        else -> {
                            Post.PRIVACY_PUBLIC
                        }
                    }
                createPost(text, imageData, privacy)
            }
        }
    }

    private fun sendMessage(composePostText: String, imageData: String?) {
        val senderId = AuthenticationHelper.currentUserId
        val recipientId = postUser?.id

        if (recipientId == null || senderId == null) {
            Log.e(
                "PostMessage",
                "Could not send message. recipientId = $recipientId. senderId = $senderId"
            )
            snack(R.string.draft_message_error)
            popBackStack()
            return
        }

        val payload = CreateMessagePayload(senderId, recipientId, composePostText, imageData ?: "")

        chatService.sendMessage(payload, object : ServiceCallback<Message> {
            override fun onSuccess(result: Message) {
                progress_bar?.hide()
                Log.i("PostMessage", "post created")
                val pictureUrl = result.pictureUrl
                try {
                    descriptionEditText?.text = null
                    imgPreview.setImageBitmap(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Log.i("PostMessage", "pic = $pictureUrl")

                popBackStack()
            }

            override fun onFailure(errMsg: String) {
                Log.e("PostMessage", "could not create post: $errMsg")
                snack(R.string.draft_message_error)
                progress_bar?.hide()
            }
        })
    }

    private fun createPost(postText: String, imageData: String?, privacy: Int) {
        val createPostRequest = when (postDraftType) {
            PostDraftType.POST -> {
                if (imageData == null) {
                    postApi.createPost(CreatePostPayload(postText, privacy = privacy))
                } else {
                    showPosting()
                    when (imageType) {
                        "image/gif" -> postImageApi.createImagePostJson(
                            ImagePostPayload(
                                postText,
                                null,
                                imageData,
                                privacy
                            )
                        )
                        else -> postImageApi.createImagePostJson(
                            ImagePostPayload(
                                postText,
                                imageData,
                                null,
                                privacy
                            )
                        )

                    }
                }
            }
            else -> {
                val membership = membership
                if (membership == null) {
                    snack(R.string.create_post_error)
                    return
                }
                if (imageData == null) {

                    if (postText.isNullOrBlank()) {
                        dismissKeyboard()
                        Toast.makeText(
                            context,
                            getString(R.string.enter_text_message),
                            Toast.LENGTH_LONG
                        ).show()
                        progress_bar?.visibility = View.GONE
                        return
                    } else {
                        showPosting()

                        communityApi.createPost(
                            membership.group.id,
                            CreateCommunityPostPayload(CreatePostPayload(postText))
                        )
                    }
                } else {
                    showPosting()
                    when (imageType) {
                        "image/gif" -> communityApi.createImagePostJson(
                            membership.group.id,
                            CreateCommunityImagePostPayload(
                                ImagePostPayload(
                                    postText,
                                    null,
                                    imageData
                                )
                            )
                        )
                        else -> communityApi.createImagePostJson(
                            membership.group.id,
                            CreateCommunityImagePostPayload(
                                ImagePostPayload(
                                    postText,
                                    imageData,
                                    null
                                )
                            )
                        )
                    }
                }

            }
        }
        compositeDisposable += createPostRequest
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .doFinally {
                // model!!.select(true)
                progress_bar?.visibility = View.GONE
            }

            .subscribe({ response ->
                context?.let { ctx ->
                    handleResponse(
                        ctx,
                        response as Response<Any?>,
                        onSuccess = {
                            Log.i(TAG, "post created")
                            val body = response.body()
                            val post = if (body is PostPayload) body.post else null

                            try {
                                descriptionEditText?.text = null
                                imgPreview.setImageBitmap(null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            post?.let {
                                execute {
                                    when (sharedPrefManager.getFeedType()
                                        ?: SharedPrefManager.MAIN_FEED_TYPE_CHRONO) {
                                        SharedPrefManager.MAIN_FEED_TYPE_CHRONO -> postFeedDao.savePosts(
                                            post,
                                            FeedSource.Type.MAIN
                                        )
                                        SharedPrefManager.MAIN_FEED_TYPE_TREND -> postFeedDao.savePosts(
                                            post,
                                            FeedSource.Type.ORDER
                                        )
                                    }
                                }
                                Log.i(TAG, "pic = ${it.picture_url}")
                            }
                            if (privacy == Post.PRIVACY_EXCLUSIVE) {
                                setTime()
                                dspTimer?.dispose()
                                startTimer()
                            }
                            if (post == null && postDraftType == PostDraftType.COMMUNITY) {
                                lastSnackbar?.dismiss()
                                snack(R.string.community_need_moderation_msg)
                            }
                            if (post?.groupId != null) {
                                properties["groupId"] = post.groupId
                            }

                            properties["type"] = privacystring(privacy)
                            if (post?.has_video != null) {
                                properties["has_video"] = post.has_video
                            }
                            if (post?.has_picture != null) {
                                properties["has_picture"] = post.has_picture
                            }
                            if (post?.has_gif != null) {
                                properties["has_gif"] = post.has_gif
                            }
                            if (postText.isNotEmpty()) {
                                properties["has_text"] = true
                            }
                            analyticsHelper.logEvent("post_created", properties)

                            popBackStack()
                        },
                        onFailure = {
                            snack(getString(R.string.generic_error_message))
                        }
                    )
                }

            }, { err ->
                context?.let { ctx ->
                    Log.e(TAG, "could not create post", err)
                    snack(err.getNetworkCallErrorMessage(ctx))
                }
            })
    }

    /**
     * Returns to correct destination depending on posting type
     */
    private fun popBackStack() {
        when (postDraftType) {
            PostDraftType.COMMUNITY -> {
                val action = CommunityFeedFragmentDirections.openCommunityFeedFragment()
                action.membership = membership
                findNavController().navigate(
                    action,
                    NavOptions.Builder().setPopUpTo(R.id.communityFeedFragment, true).build()
                )
            }
            PostDraftType.MESSAGE -> {
                try {
                    findNavController().navigate(
                        R.id.chatFragment,
                        ChatFragmentArgs.Builder()
                            .setRecipient(postUser)
                            .build().toBundle(),
                        navOptions {
                            popUpTo(R.id.recentContactsFragment) { inclusive = false }
                        })
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            else -> findNavController().popBackStack(
                R.id.mainFeedFragment,
                false
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.post_content -> {
                handlePost()
                return true
            }
            R.id.menu_edit_text -> {
                addMediaToPost(R.id.textMediaFragment)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.draft_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Handler for Post button click
     */
    private fun handlePost() {
        dismissKeyboard()

        val message = descriptionEditText?.text.toString()

        if (postDraftType != PostDraftType.MESSAGE && message.hashtags().size > PostApi.MAX_HASHTAG_COUNT) {
            snack(R.string.create_post_hashtag_error)
            progress_bar.hide()
            return
        }

        // remove draft array
        MainActivity.draftFiles.clear()

        sharedViewModel?.select(true)
        lastSnackbar = Snackbar.make(
            requireView(),
            R.string.draft_message_posting,
            Snackbar.LENGTH_SHORT
        )
        lastSnackbar?.show()

        when {
            (videoPath != null) -> {
                postVideo(videoPath as String, message)
            }
            (videoContentUri != null) -> {
                postVideoPath(Uri.parse(videoContentUri), message)
            }
            else -> {
                try {
                    proceed()
                } catch (e: Exception) {
                    snack(R.string.image_too_large)
                    Log.e(TAG, "onOptionsItemSelected", e)
                }
            }
        }
        progress_bar.hide()
    }

    private fun rotateImg(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg =
            Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    private fun dismissKeyboard() {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    companion object {
        private const val MAX_IMAGE_SIZE = 9437184L
        private const val MAX_RESOLUTION = 1024
        private const val STATE_POST_TEXT = "composeText"
    }

    fun showPosting() {
        lastSnackbar = Snackbar.make(
            requireView(),
            R.string.draft_message_posting,
            Snackbar.LENGTH_SHORT
        )
        lastSnackbar?.show()
    }

    private fun startTimer() {
        Observable.interval(0, 3000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.Observer<Long?> {
                override fun onComplete() {

                }

                override fun onSubscribe(d: Disposable) {
                    dspTimer = d
                }

                override fun onNext(t: Long) {
                    val todayAsString = dateFormat.format(System.currentTimeMillis())
                    sharedPrefManager.getExclusivePostTime()?.let { tomorrowAsString ->
                        if (tomorrowAsString.equals(todayAsString, true)
                                .not() && tomorrowAsString.isNotEmpty()
                        ) {
                            sharedPrefManager.setExclusivePostTime("")
                        } else {
                        }
                    } ?: kotlin.run {

                    }

                    sharedPrefManager.getLaunchTime()?.let { launchtime ->
                        if (launchtime.equals(todayAsString, true).not()) {
                            sharedPrefManager.setSupportPostId("")
                            sharedPrefManager.setLaunchTime(
                                SimpleDateFormat("yyyy-MM-dd").format(
                                    System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                override fun onError(e: Throwable) {

                }
            })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            sharedViewModel?.select(false)
            findParentNavController().navigate(R.id.postTypesFragment)
        }
    }
}