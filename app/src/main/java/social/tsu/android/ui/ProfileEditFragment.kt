package social.tsu.android.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.yalantis.ucrop.UCrop
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.profile_edit_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.*
import social.tsu.android.network.model.SearchUser
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.DefaultSearchService
import social.tsu.android.service.DefaultUserProfileImageService
import social.tsu.android.service.SearchService
import social.tsu.android.service.SearchServiceCallabck
import social.tsu.android.ui.ext.afterTextChanged
import social.tsu.android.ui.search.OnItemClickListener
import social.tsu.android.ui.search.SearchResultsAdapter
import social.tsu.android.utils.*
import social.tsu.android.viewModel.profileEdit.DefaultProfileEditViewModel
import social.tsu.android.viewModel.profileEdit.ProfileEditViewModelCallback
import java.io.File

class ProfileEditFragment: Fragment(), ProfileEditTextWatcherCallback, ProfileEditViewModelCallback,
    AdapterView.OnItemSelectedListener, SearchServiceCallabck, CoroutineScope by MainScope() {

    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
        private const val COVER_PERMISSIONS_REQUEST_CODE = 601
        private const val PROFILE_PERMISSIONS_REQUEST_CODE = 602
    }


    val args by navArgs<ProfileEditFragmentArgs>()

    private var mToast: Toast? = null
    private enum class CameraCaptureMode {
        unknown,
        profile,
        cover
    }

    private var mode: CameraCaptureMode = CameraCaptureMode.unknown


    private var backCallback: OnBackPressedCallback? = null

    private lateinit var outputDirectory: File
    private lateinit var outputFile: File
    private lateinit var tempFile: File

    private val viewModel: DefaultProfileEditViewModel by lazy {
        DefaultProfileEditViewModel(activity?.application as TsuApplication, this)
    }

    private val profileImageService: DefaultUserProfileImageService by lazy {
        //changed this to variable to avoid NPE
        DefaultUserProfileImageService(application)
    }

    private val searchService: SearchService by lazy {
        DefaultSearchService(activity?.application as TsuApplication, this)
    }

    private val searchResultsAdapter = SearchResultsAdapter(object : OnItemClickListener<Any> {
        override fun onItemClicked(
            item: View,
            searchUser: Any
        ) {
            if (searchUser is SearchUser) {
                Log.d("CLICK", "You clicked ${searchUser.username}")
                userSelectedRelationshipUser(searchUser)
            }
        }
    })

    private var progressBar: View? = null

    private lateinit var application: TsuApplication


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.let {
            application = it.application as TsuApplication
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile_edit_fragment, container, false)

        outputDirectory = MainActivity.getOutputDirectory(requireActivity())

        outputFile = CameraUtil.createFile(
            outputDirectory,
            FILENAME,
            PHOTO_EXTENSION
        )


        progressBar = view?.findViewById(R.id.full_screen_progress_overlay)

        view.findViewById<EditText>(R.id.settings_bio_edit)
            .addTextChangedListener(ProfileEditTextWatcher(ProfileEditTextWatcherType.bio, this))
        view.findViewById<EditText>(R.id.settings_website_edit).addTextChangedListener(
            ProfileEditTextWatcher(
                ProfileEditTextWatcherType.website,
                this
            )
        )
        view.findViewById<EditText>(R.id.settings_youtube_edit).addTextChangedListener(
            ProfileEditTextWatcher(
                ProfileEditTextWatcherType.youtube,
                this
            )
        )
        view.findViewById<EditText>(R.id.settings_instagram_edit).addTextChangedListener(
            ProfileEditTextWatcher(
                ProfileEditTextWatcherType.instagram,
                this
            )
        )
        view.findViewById<EditText>(R.id.settings_facebook_edit).addTextChangedListener(
            ProfileEditTextWatcher(
                ProfileEditTextWatcherType.facebook,
                this
            )
        )

        val realtionshipWithEditText =
            view.findViewById<EditText>(R.id.relationship_with_textfield_edit)
        realtionshipWithEditText.isFocusable = true
        realtionshipWithEditText.isFocusableInTouchMode = true
        realtionshipWithEditText.inputType = InputType.TYPE_NULL
        realtionshipWithEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showSearch()
            }
        }

        view.findViewById<View>(R.id.settings_profile_picture).setOnClickListener {
            openCropProfileImageCapture()
        }
        view.findViewById<View>(R.id.settings_cover_picture).setOnClickListener {
            openCropCoverImageCapture()
        }
        view.findViewById<Button>(R.id.settings_save_button).setOnClickListener {
            dismissKeyboard()
            progressBar?.show()
            viewModel.updateProfile()
        }

        view.findViewById<Spinner>(R.id.settings_relationship_status).onItemSelectedListener = this

        val bioTextInputLayout = view.findViewById<TextInputLayout>(R.id.bio)
        view.findViewById<TextInputEditText>(R.id.settings_bio_edit).afterTextChanged {
            if (it.length >= 160)
                showError(bioTextInputLayout)
            else
                hideError(bioTextInputLayout)
        }

        initBackCallback()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initOldUserDetails()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                return if (settings_search_container?.visibility == View.VISIBLE) {
                    hideSearch()
                    true
                } else {
                    false
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initOldUserDetails() {
        args.oldUserDetails?.let {
            settings_bio_edit?.setText(it.bio)
            settings_website_edit?.setText(it.website)
            settings_youtube_edit?.setText(it.youtube)
        }

        onBackPressed(true) {
            closeProfileEdit()
        }
    }

    private fun initBackCallback() {
        if (backCallback != null) {
            return
        }
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                Log.d("EditFragment", "callback!!")
                if (settings_search_container?.visibility == View.VISIBLE) {
                    hideSearch()
                }

            }

        }
        backCallback?.isEnabled = false
        requireActivity().onBackPressedDispatcher.addCallback(backCallback as OnBackPressedCallback)
    }

    override fun onStart() {
        super.onStart()
        if (mode == CameraCaptureMode.unknown) {
            AuthenticationHelper.currentUserId?.let {
                viewModel.getUserInfo(it)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        dismissKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancel()
        backCallback?.remove()
        mToast?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && data != null) {
            val result = UCrop.getOutput(data!!)
            if (resultCode == Activity.RESULT_OK) {
                val uri = result
                when(mode) {
                    CameraCaptureMode.profile -> {
                        updateProfilePhoto(uri)
                        viewModel.updateProfilePicture(uri)
                    }
                    CameraCaptureMode.cover -> {
                        updateCoverPhoto(uri)
                        viewModel.updateCoverPicture(uri)
                    }
                }

                outputFile.delete()
                outputFile = CameraUtil.createFile(
                    outputDirectory,
                    FILENAME,
                    PHOTO_EXTENSION
                )
            } else if (resultCode == UCrop.RESULT_ERROR) {
                val error = UCrop.getError(data)
            }
        }

        if (resultCode == Activity.RESULT_OK) {
            when(requestCode){
                PickerUtils.PICK_IMAGE_CHOOSER_REQUEST_CODE -> {
                    val imageUri = PickerUtils.getPickImageResultUri(requireContext(), data)
                    if(mode == CameraCaptureMode.profile){
                        openProfileImageCropper(imageUri!!)
                    } else {
                        openCoverCropper(imageUri!!)
                    }
                }
            }
        }

    }

    override fun completedUserSearch(users: List<SearchUser>) {
        Log.d("SEARCH", "response = ${users}")
        launch {
            searchResultsAdapter.updateSearchResults(users)
        }
    }

    override fun didUpdateText(type: ProfileEditTextWatcherType, value: String) {
        when(type) {
            ProfileEditTextWatcherType.bio -> viewModel.updateBio(value)
            ProfileEditTextWatcherType.website -> viewModel.updateWebsite(value)
            ProfileEditTextWatcherType.youtube -> viewModel.updateYoutube(value)
            ProfileEditTextWatcherType.twitter -> viewModel.updateTwitter(value)
            ProfileEditTextWatcherType.instagram -> viewModel.updateInstagram(value)
            ProfileEditTextWatcherType.facebook -> viewModel.updateFacebook(value)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.updateRelationshipStatus(position)
        if (position >= 2) {
            this.view?.findViewById<LinearLayout>(R.id.settings_relationship_with_container)?.visibility = View.VISIBLE
        } else {
            this.view?.findViewById<LinearLayout>(R.id.settings_relationship_with_container)?.visibility = View.GONE
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun didEndEditing() {
        dismissKeyboard()
    }

    private fun updateProfilePhoto(uri: Uri?) {
        view?.findViewById<CircleImageView>(R.id.settings_profile_picture)?.setImageURI(uri)
    }

    private fun updateCoverPhoto(uri: Uri?) {
        view?.findViewById<ImageView>(R.id.settings_cover_picture)?.setImageURI(uri)
    }

    private fun openCropCoverImageCapture() {
        val context = this.context?: return

        mode = CameraCaptureMode.cover


        checkPermissionsInFragment(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
            COVER_PERMISSIONS_REQUEST_CODE
        ) {
            PickerUtils.startPickImageChooser(context, this)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            PROFILE_PERMISSIONS_REQUEST_CODE,
            COVER_PERMISSIONS_REQUEST_CODE -> PickerUtils.startPickImageChooser(requireContext(), this)
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private fun openCropProfileImageCapture() {
        val context = this.context?: return

        mode = CameraCaptureMode.profile

        checkPermissionsInFragment(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
            PROFILE_PERMISSIONS_REQUEST_CODE
        ) {
            PickerUtils.startPickImageChooser(context, this)
        }

    }

    private fun openCoverCropper(sourceUri: Uri) {
        UCrop.of(sourceUri, Uri.fromFile(outputFile))
            .withAspectRatio(21f, 7f)
            .start(requireContext(), this)
    }

    private fun openProfileImageCropper(sourceUri: Uri) {
        UCrop.of(sourceUri, Uri.fromFile(outputFile))
            .withAspectRatio(1f, 1f)
            .withOptions(UCrop.Options().apply { setCircleDimmedLayer(true)})
            .start(requireContext(), this)
    }

    override fun didErrorWith(message: String) {
        snack(message)
        progressBar?.hide()
    }

    override fun loadProfile(profile: UserProfile) {
        updateUserInfo(profile)
        progressBar?.hide()
    }

    override fun completedProfileUpdate(profile: UserProfile) {
        if (context == null) return
        //check if activity is not null as image service will crash otherwise
        if (activity == null) return
        loadProfile(profile)
        closeProfileEdit()
    }

    private fun closeProfileEdit() {
        if (args.isNewUser) {
            findNavControllerOrNull()?.navigate(R.id.mainFeedFragment)
        } else {
            findNavControllerOrNull()?.navigateUp()
        }
    }

    private fun userSelectedRelationshipUser(user: SearchUser) {
        updateRelationshipWithLabel(user.fullName)
        viewModel.updateRelationshipWith(user.id)
        hideSearch()
    }

    private fun updateUserInfo(profile: UserProfile) {
        if (activity == null) {
            Log.d("ProfileEditFragment", "activity is null, wrapping up!")
            return
        }
        updateProfilePhoto(profile.profilePictureUrl)
        updateCoverPhoto(profile.coverPictureUrl)
        updateBio(profile.bio)
        updateWebsite(profile.website)
        updateYoutube(profile.youtube)
        updateInstagram(profile.instagram ?: "")
        updateFacebook(profile.facebook ?: "")
        updateRelationshipStatus(profile)
    }

    private fun updateRelationshipStatus(profile: UserProfile) {
        val status = profile.relationshipStatus ?: return
        context?.resources?.getStringArray(R.array.relationship_status)?.let {
            val item = it.indexOf<String>(status)
            settings_relationship_status?.setSelection(item, true)
            if (item >= 2) {
                updateRelationshipWith(profile.relationshipWithId)
            }
        }
    }

    private fun updateBio(value: String?) {
        settings_bio_edit?.setText(value)
    }

    private fun updateWebsite(value: String) {
        settings_website_edit?.setText(value)
    }

    private fun updateYoutube(value: String) {
        settings_youtube_edit?.setText(value)
    }

    private fun updateInstagram(value: String) {
        settings_instagram_edit?.setText(value)
    }

    private fun updateFacebook(value: String) {
        settings_facebook_edit?.setText(value)
    }

    private fun updateRelationshipWithLabel(value: String) {
        relationship_with_textfield_edit?.setText(value)
    }

    private fun updateRelationshipWith(value: Int?) {
        if (value != null) {
            viewModel.updateRelationshipWith(value)
        }
        viewModel.currentRelationshipUser?.let {
            updateRelationshipWithLabel(it.fullName)
        }
    }

    private fun updateProfilePhoto(key: String?) {
        profileImageService.getCoverPicture(key, false) {
            it?.let {
                settings_profile_picture?.setImageDrawable(it)
            } ?: run {
                settings_profile_picture?.setImageResource(R.drawable.user)
            }
        }
    }

    private fun updateCoverPhoto(key: String?) {
        profileImageService.getCoverPicture(key, false) {
            it?.let {
                settings_cover_picture?.setImageDrawable(it)
            } ?: run {
                settings_cover_picture?.setImageResource(R.drawable.ic_no_cover_small)

            }
        }
    }

    private fun hideSearch() {
        backCallback?.isEnabled = false
        settings_scroll_view?.visibility = View.VISIBLE
        settings_search_container?.visibility = View.GONE
    }

    private fun showSearch() {
        backCallback?.isEnabled = true
        settings_scroll_view?.visibility = View.GONE
        settings_search_container?.visibility = View.VISIBLE
        settings_search_bar?.isIconifiedByDefault = true
        settings_search_bar?.isFocusable = true
        settings_search_bar?.isIconified = false
        settings_search_bar?.requestFocusFromTouch()

        val viewManager = LinearLayoutManager(context)
        settings_search_recycler_view?.apply {
            layoutManager = viewManager
            adapter = searchResultsAdapter
        }

        settings_search_bar?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (requireActivity().isInternetAvailable()) {
                    val term = newText ?: return true
                    Log.i("SEARCH", "after ${term}")
                    val trim = term.trim()
                    searchService.searchUsers(trim)
                } else {
                    if (mToast != null)
                        mToast?.cancel()
                    mToast = Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.connectivity_issues_message),
                        Toast.LENGTH_LONG
                    )
                    mToast?.show()
                }
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {

                return true
            }
        })

        settings_search_bar?.setOnCloseListener {
            hideSearch()
            return@setOnCloseListener true
        }
    }

    private fun showError(bioTextInputLayout: TextInputLayout) {
        bioTextInputLayout.isErrorEnabled = true
        bioTextInputLayout.error = getString(R.string.bio_error_message)
    }

    private fun hideError(bioTextInputLayout: TextInputLayout) {
        bioTextInputLayout.error = null
    }

}