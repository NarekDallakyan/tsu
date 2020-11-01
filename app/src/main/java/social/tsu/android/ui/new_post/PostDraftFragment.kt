package social.tsu.android.ui.new_post
import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
import social.tsu.android.ui.post_feed.community.CommunityFeedFragmentDirections
import social.tsu.android.ui.post_feed.main.MainFeedFragmentDirections
import social.tsu.android.ui.search.MENTION_TYPE
import social.tsu.android.ui.search.SearchFragment
import social.tsu.android.utils.*
import social.tsu.android.viewModel.MentionViewModel
import social.tsu.android.viewModel.SharedViewModel
import social.tsu.android.workmanager.workers.UploadVideoWorker
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
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

    var textIndex: Int = 0
    val properties = HashMap<String, Any?>()

    @IdRes
    var defaultPostType: Int = R.id.photoCaptureFragment2

    private var compositeDisposable = CompositeDisposable()
    private var composeEditText: EditText? = null

    //val args: PostDraftFragmentArgs by navArgs()

    private var lastSnackbar: Snackbar? = null

    private var visibilityRadioGroup: RadioGroup? = null
    private var visibilityMessage: TextView? = null

    var searchOpen = false
    private var mentionViewModel: MentionViewModel? = null

    var initialText: String? = null


    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {

        }

        override fun beforeTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int
        ) {

        }

        override fun afterTextChanged(arg0: Editable) {
            val value = arg0.toString()
            if (value.isNullOrEmpty()) {
                return
            }

            if (value.contains("@")) {
                val split = value.split(" ")
                for (item in split) {
                    if (item.contains("@") && item.length == 1) {
                        searchOpen = true
                        composeEditText?.let {
                            textIndex = it.selectionEnd
                            initialText = it.text.toString()
                        }
                        openNavController()
                        break
                    }
                }
            }

        }
    }

    override fun onDestroyView() {
        compositeDisposable.dispose()
        dspTimer?.dispose()
        super.onDestroyView()
        //TODO Fix code below as its creating video issues.
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Get argument data
        getArgumentData()

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        mentionViewModel = ViewModelProvider(requireActivity()).get(MentionViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        //model!!.select(false)

        val view = inflater.inflate(R.layout.draft_post, container, false)

        composeEditText = view.findViewById(R.id.composePost)

        composeEditText?.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    composeEditText?.addTextChangedListener(textWatcher)
                } else {
                    composeEditText?.removeTextChangedListener(textWatcher)
                }
            }

        //Set up "Add photo" button
        view.findViewById<View>(R.id.btn_add_photo).setOnClickListener {
            addImageToPost()
        }

        //Set up "Add Video" button
        view.findViewById<View>(R.id.btn_add_video).setOnClickListener {
            addVideoToPost()
        }

        when (postDraftType) {
            PostDraftType.POST -> {
                visibilityRadioGroup = view.findViewById(R.id.compose_post_radio_group)
                visibilityMessage = view.findViewById(R.id.compose_post_visibility_msg)
                visibilityRadioGroup?.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.compose_post_private -> {
                            visibilityMessage?.setText(R.string.create_post_private_msg)
                        }
                        R.id.compose_post_public -> {
                            visibilityMessage?.setText(R.string.create_post_public_msg)
                        }
                        R.id.compose_post_exclusive -> {
                            visibilityMessage?.setText(R.string.create_post_public_exclusive_msg)
                        }
                    }
                }

                view.findViewById<Button>(R.id.compose_post_visibility_group).show()
            }
            PostDraftType.MESSAGE -> {
                view.findViewById<View>(R.id.btn_add_video)?.hide()
            }
            else -> {
            }
        }



        setHasOptionsMenu(true)
        return view
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

    private fun getArgumentData() {

        if (arguments == null) return

        videoPath = requireArguments().getString("videoPath")
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
        var tag = ""
        mentionViewModel?.getTag()?.observe(requireActivity(), Observer {
            if (!it.isNullOrEmpty()) tag = it
        })

        mentionViewModel?.selectTag("")

        if (!initialText.isNullOrEmpty()) {
            if (!tag.isNullOrEmpty()) {
                composeEditText?.setText(initialText + tag)
            } else {
                composeEditText?.setText(initialText)
            }
        }
        initialText = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        composeEditText?.setText("")
        mentionViewModel?.selectTag("")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getString(STATE_POST_TEXT)?.let { composePost?.setText(it) }
        dateFormat = SimpleDateFormat("yyyy-MM-dd")
        startTimer()
    }

    override fun onStart() {
        super.onStart()

        composeEditText?.setText("")
        composeEditText?.text?.let {
            if (it.toString().isBlank()) {
                view?.findViewById<EditText>(R.id.composePost)
                    ?.setText(SpannableString(postText))
            }
        }

        //Handle photo and video URIs if any of them are available.
        //URIs comes in fragment arguments typically from PostTypesFragment after it's done with capture
        //or selection

        photoUri?.let {
            try {
                handlePhoto(Uri.parse(it))
            } catch (e: Exception) {
                snack(getString(R.string.unable_to_load_attached_photo))
            }
            return
        }

        videoPath?.let {
            handleVideo(it)
            return
        }
        videoContentUri?.let {
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
        val privacy = when (visibilityRadioGroup?.checkedRadioButtonId) {
            R.id.compose_post_private -> {
                Post.PRIVACY_PRIVATE
            }
            R.id.compose_post_public -> {
                Post.PRIVACY_PUBLIC
            }
            R.id.compose_post_exclusive -> {
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
        //findNavController().navigate(PostDraftFragmentDirections.actionPostDraftFragmentToMainFeedFragment())
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

        val privacy = when (visibilityRadioGroup?.checkedRadioButtonId) {
            R.id.compose_post_private -> {
                Post.PRIVACY_PRIVATE
            }
            R.id.compose_post_public -> {
                Post.PRIVACY_PUBLIC
            }
            R.id.compose_post_exclusive -> {
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

            val imgPreview = view?.findViewById<ImageView>(R.id.imgPreview)

            val videoFile = File(videoPath)
            val uri = Uri.fromFile(videoFile)

            imgPreview?.let {
                Glide.with(requireActivity()).load(uri).thumbnail(0.1f).into(it)
            }
        } catch (e: IOException) {
            Log.e("IMAGE", "error reading stream", e)

        }
    }

    fun openPostTypesFragment() {
        addMediaToPost(defaultPostType)
    }

    /**
     * Convenience method to open PostTypesFragment with photo tab active
     */
    private fun addImageToPost() {
        addMediaToPost(R.id.photoCaptureFragment2)
    }

    /**
     * Convenience method to open PostTypesFragment with video tab active
     */
    private fun addVideoToPost() {
        addMediaToPost(R.id.videoCaptureFragment2)
    }

    /**
     * Opens PostTypesFragment with specific source type preselected
     *
     * @param mediaType id of a fragment that should be preselected when media capture
     * interface opens
     */
    private fun addMediaToPost(mediaType: Int) {
        composeEditText?.let {
            initialText = it.text.toString()
        }
        defaultPostType = mediaType
        checkPermissions(
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ),
            MainActivity.NEW_POST_FROM_FEED_PERMISSIONS_REQUEST_CODE
        ) {
            composeEditText?.let {
                //Filling in all arguments for PostTypesFragment:
                //setting defaultPostType (which tab should be selected on launch)
                //and copying items from current fragment's arguments to pass them forward to PostTypesFragment
                val text = it.text.toString()
                val action = MainFeedFragmentDirections
                    .showPostTypesFragment(text).apply {
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

            val imgPreview = view?.findViewById<ImageView>(R.id.imgPreview)

            val uri = Uri.parse(path)

            imgPreview?.let {
                Glide.with(requireActivity()).load(uri).thumbnail(0.1f).into(imgPreview)
            }


        } catch (e: IOException) {
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

            view?.let {
                Glide.with(imgPreview)
                    .load(bitmap)
                    .into(imgPreview)
            }

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
        val text = composeEditText?.text.toString()
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
                    when (visibilityRadioGroup?.checkedRadioButtonId) {
                        R.id.compose_post_private -> {
                            Post.PRIVACY_PRIVATE
                        }
                        R.id.compose_post_public -> {
                            Post.PRIVACY_PUBLIC
                        }
                        R.id.compose_post_exclusive -> {
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
                    composeEditText?.text = null
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
                            val post = if (body is PostPayload) body.post  else null

                            try {
                                composeEditText?.text = null
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

    private fun makeImageBody(bytes: ByteArray, isGif: Boolean): MultipartBody.Part {
        val reqFile: RequestBody =
            bytes.toRequestBody(if (isGif) "image/gif".toMediaTypeOrNull() else "image/jpeg".toMediaTypeOrNull())

        val body: MultipartBody.Part = MultipartBody.Part.createFormData(
            if (isGif) "gif" else "picture",
            if (isGif) "gif.gif" else "picture.jpg",  // filename, this is optional
            reqFile
        )
        return body
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        initialText = composeEditText?.text.toString()
        outState.putString(STATE_POST_TEXT, initialText)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.post_content -> {
                onPostClick()
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
    private fun onPostClick() {
        dismissKeyboard()

        val view = requireView()
        val message = view.findViewById<TextView>(R.id.composePost)?.text.toString()

        if (postDraftType != PostDraftType.MESSAGE && message.hashtags().size > PostApi.MAX_HASHTAG_COUNT) {
            snack(R.string.create_post_hashtag_error)
            return
        }

        sharedViewModel!!.select(true)
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

    fun openNavController() {

        initialText = composeEditText?.text.toString()
        composeEditText?.isSelected = false
        composeEditText?.isFocusable = false


        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.postDraftFragment) {
            navController.navigate(
                R.id.action_postDraft_to_mentionFragment,
                bundleOf(
                    "searchType" to SearchFragment.SEARCH_TYPE_MENTION,
                    MENTION_TYPE to SearchFragment.MENTION_TYPE_COMMUNITY
                )
            )
        }

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
                            compose_post_exclusive.visibility = View.VISIBLE
                            sharedPrefManager.setExclusivePostTime("")
                        } else {
                            compose_post_exclusive.visibility =
                                if (tomorrowAsString.isEmpty()) View.VISIBLE else View.GONE
                        }
                    } ?: kotlin.run {
                        compose_post_exclusive.visibility = View.VISIBLE
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
}