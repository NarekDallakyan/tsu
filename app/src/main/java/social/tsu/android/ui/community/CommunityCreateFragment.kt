package social.tsu.android.ui.community

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.textfield.TextInputLayout
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_community_create.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.PickerUtils
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.Group
import social.tsu.android.service.CommunityType
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.*
import social.tsu.android.ui.search.SearchFragment
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.community.CommunityCreateViewModel
import social.tsu.android.viewModel.community.CommunityCreateViewModelCallback
import social.tsu.android.viewModel.community.DefaultCommunityCreateViewModel
import java.io.File

class CommunityCreateFragment() : Fragment(), CommunityCreateViewModelCallback {

    constructor(args: CommunityCreateFragmentArgs) : this() {
        arguments = args.toBundle()
    }

    companion object {
        private const val MAX_NAME_SIZE: Int = 79
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
        private const val COMMUNITY_PERMISSIONS_REQUEST_CODE = 603
    }

    private val viewModel: CommunityCreateViewModel by lazy {
        DefaultCommunityCreateViewModel(activity?.application as TsuApplication, this)
    }

    private val model: CommunityViewModel by activityViewModels()

    private lateinit var name: EditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var description: EditText
    private lateinit var topic: EditText
    private lateinit var contentModeration: SwitchCompat
    private lateinit var communityType: RadioGroup
    private lateinit var coverPictureButton: LinearLayout
    private lateinit var coverPictureError: TextView
    private lateinit var coverPicture: ImageView
    private lateinit var coverPictureLabel: TextView
    private lateinit var coverPictureContainer: View
    private lateinit var createCommunity: Button
    private lateinit var deleteCommunity: Button
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var createDescription: TextView
    private lateinit var createBtnContainer: View

    private lateinit var outputDirectory: File
    private lateinit var outputFile: File

    private lateinit var community: Group

    //CREATE mode by default
    private var currentMode = CreateCommunityMode.CREATE

    val args by navArgs<CommunityCreateFragmentArgs>()

    enum class CreateCommunityMode {
        CREATE, EDIT
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community_create, container, false)

        if (args != null && args.mode != null) {
            currentMode = args.mode
        }

        outputDirectory = MainActivity.getOutputDirectory(requireActivity())

        outputFile = CameraUtil.createFile(
            outputDirectory,
            FILENAME,
            PHOTO_EXTENSION
        )

        name = view.findViewById(R.id.community_name_edit)
        nameLayout = view.findViewById(R.id.community_name_layout)
        coverPictureButton = view.findViewById(R.id.add_picture_button)
        coverPictureError = view.findViewById(R.id.cover_picture_error)
        coverPicture = view.findViewById(R.id.cover_picture)
        coverPictureLabel = view.findViewById(R.id.cover_picture_label)
        coverPictureContainer = view.findViewById(R.id.cover_picture_container)
        description = view.findViewById(R.id.description_edit)
        topic = view.findViewById(R.id.topic_edit)
        contentModeration = view.findViewById(R.id.content_moderation_switch)
        communityType = view.findViewById(R.id.community_type_radiogroup)
        createCommunity = view.findViewById(R.id.create_button)
        deleteCommunity = view.findViewById(R.id.delete_button)
        scrollView = view.findViewById(R.id.scrollView)
        progressBar = view.findViewById(R.id.progress_bar)
        createDescription = view.findViewById(R.id.create_community_description)
        createBtnContainer = view.findViewById(R.id.create_button_container)

        initTopicSelectorButton()
        initCoverPictureButton()

        name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                editable?.let {
                    if (it.length > MAX_NAME_SIZE) {
                        name.removeTextChangedListener(this)
                        it.delete(MAX_NAME_SIZE, it.length)
                        name.addTextChangedListener(this)
                        nameLayout.error = getString(R.string.community_name_error)
                    } else {
                        nameLayout.error = ""
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })


        if (currentMode == CreateCommunityMode.EDIT) {
            initAsEditor()
        } else {
            createBtnContainer.background = null
        }

        initCreateCommunityButton()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        community_type_radiogroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btn_radio_private -> {
                    community_compose_visibility_msg?.setText(R.string.community_private_msg)
                }
                R.id.btn_radio_public -> {
                    community_compose_visibility_msg?.setText(R.string.community_public_msg)
                }
                R.id.btn_radio_exclusive -> {
                    community_compose_visibility_msg?.setText(R.string.community_public_exclusive_msg)
                }
            }
        }

    }

    private fun initAsEditor() {
        if (args.community != null) {
            createDescription.hide()
            community = args.community!!
            description.setText(community.description)
            name.setText(community.name)
            communityType.check(
                when (community.visibility) {
                    "open" -> R.id.btn_radio_public
                    "restricted" -> {
                        communityType.findViewById<RadioButton>(R.id.btn_radio_public).isEnabled =
                            false
                        R.id.btn_radio_private
                    }
                    "exclusive" -> {
                        communityType.findViewById<RadioButton>(R.id.btn_radio_public).isEnabled =
                            false
                        communityType.findViewById<RadioButton>(R.id.btn_radio_private).isEnabled =
                            false
                        R.id.btn_radio_exclusive
                    }
                    else -> R.id.btn_radio_public
                }
            )
            createCommunity.setText(R.string.community_edit_update)

            topic.setText(community.parentName)
            if (model.selectedGroup == null) {
                model.selectedGroup =
                    Group.createMockGroupForEdit(community.parentId, community.parentName ?: "")
            }

            contentModeration.isChecked = community.requireModeration

            deleteCommunity.show()
            deleteCommunity.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(getString(R.string.confirm))
                    .setMessage(getString(R.string.community_delete_message))
                    .setPositiveButton(
                        R.string.delete
                    ) { dialog, which ->
                        deleteCommunity()
                        dialog.dismiss()
                    }
                    .setNegativeButton(
                        R.string.cancel
                    ) { dialog, which ->
                        dialog.dismiss()
                    }
                builder.show()
            }

            coverPictureError.hide()

            val url = community.pictureUrl.split("/groups").last()
            Glide.with(this).asFile()
                .load(formatUrl("/groups$url"))
                .into(object : CustomTarget<File>() {
                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        coverPicture.show()
                        coverPictureButton.hide()
                        coverPicture.setImageURI(Uri.fromFile(resource))
                        model.pictureUri = Uri.fromFile(resource)
                    }
                })
        }
    }

    private fun formatUrl(source: String): String {
        if (source.startsWith("/")) {
            return "${HostProvider.imageHost}${source}".replace("square", "cover")
        }

        return source
    }

    private fun deleteCommunity() {
        if (requireActivity().isInternetAvailable()) {
            progressBar.show()
            viewModel.deleteCommunity(community.id)
        } else
            requireActivity().internetSnack()
    }

    private fun initCreateCommunityButton() {
        createCommunity.setOnClickListener {
            val type = when (communityType.checkedRadioButtonId) {
                R.id.btn_radio_public -> CommunityType.OPEN
                R.id.btn_radio_private -> CommunityType.RESTRICTED
                else -> CommunityType.EXCLUSIVE
            }

            if (name.text.isNotEmpty() && description.text.isNotEmpty() && model.selectedGroup != null
                && (model.pictureUri != null || currentMode == CreateCommunityMode.EDIT)
            ) {
                if (requireActivity().isInternetAvailable()) {
                    progressBar.show()

                    if (currentMode == CreateCommunityMode.CREATE) {
                        viewModel.createCommunity(
                            name.text.toString(),
                            description.text.toString(),
                            model.selectedGroup!!.id,
                            contentModeration.isChecked,
                            type,
                            model.pictureUri!!
                        )
                    } else {
                        viewModel.updateCommunity(
                            community.id,
                            name.text.toString(),
                            description.text.toString(),
                            model.selectedGroup!!.id,
                            contentModeration.isChecked,
                            type,
                            model.pictureUri!!
                        )
                    }
                } else
                    requireActivity().internetSnack()


            } else {
                if (name.text.isEmpty()) {
                    name.error = getString(R.string.community_create_validate_error)
                }
                if (description.text.isEmpty()) {
                    description.error = getString(R.string.community_create_validate_error)
                }
                if (model.selectedGroup == null) {
                    topic.error = getString(R.string.community_create_validate_error)
                }
                if (model.pictureUri == null) {
                    showCoverPictureError()
                    scrollView.smoothScrollTo(0, coverPictureButton.bottom)
                }
            }
        }
    }

    override fun didCommunityCreate() {
        snack("Community created")
        model.pictureUri = null
        model.selectedGroup = null
        model.triggerCommunityCreate()
        resetUI()
    }

    private fun resetUI() {
        progressBar.hide()
        name.setText("")
        description.setText("")
        model.pictureUri = null
        coverPicture.setImageURI(null)
        coverPicture.setVisibleOrGone(false)
        coverPictureButton.setVisibleOrGone(true)
        communityType.check(R.id.btn_radio_public)
        communityType.findViewById<RadioButton>(R.id.btn_radio_public).isEnabled = true
        communityType.findViewById<RadioButton>(R.id.btn_radio_private).isEnabled = true
        contentModeration.isChecked = false
        hideCoverPictureError()
        model.selectedGroup = null
        topic.setText(R.string.communit_create_topic_text)
    }

    fun hideCoverPictureError() {
        coverPictureError.hide()
        coverPictureButton.setBackgroundResource(R.drawable.bg_add_picture_button)
    }

    fun showCoverPictureError() {
        coverPictureError.show()
        coverPictureButton.setBackgroundResource(R.drawable.bg_add_picture_button_error)
    }

    override fun didFailCommunityCreate(message: String) {
        progressBar.hide()
        snack(message)
    }

    override fun didCommunityUpdate(group: Group) {
        progressBar.hide()
        didCommunityChanged(group)
        view?.post {
            findNavController().popBackStack()
        }
    }

    private fun didCommunityChanged(group: Group) {
        snack(R.string.community_update_success)
        model.pictureUri = null
        model.selectedGroup = null
        model.triggerCommunityChanged(group)
        resetUI()
    }

    private fun didCommunityDeleted() {
        snack(R.string.community_delete_success)
        model.pictureUri = null
        model.selectedGroup = null
        model.triggerCommunityDeleted()
        resetUI()
    }

    override fun didCommunityDelete() {
        didCommunityDeleted()
        view?.post {
            findNavController().popBackStack(R.id.communityFragment, false)
        }
    }

    private fun initCoverPictureButton() {
        coverPictureButton.setOnClickListener {
            launchPictureChooser()
        }
        coverPicture.setOnClickListener {
            launchPictureChooser()
        }
    }

    private fun launchPictureChooser() {
        val context = this.context?: return
        checkPermissionsInFragment(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
            COMMUNITY_PERMISSIONS_REQUEST_CODE
        ) {
            PickerUtils.startPickImageChooser(context, this)
        }

    }

    override fun onStart() {
        super.onStart()
        model.selectedGroup?.let {
            topic.setText(it.name)
            topic.error = null
        } ?: run {
            topic.setText(R.string.communit_create_topic_text)
        }

        model.pictureUri?.let {
            coverPictureButton.hide()
            coverPicture.show()
            coverPicture.setImageURI(it)
        } ?: run {
            coverPictureButton.show()
            coverPicture.hide()
            coverPicture.setImageURI(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.selectedGroup = null
        model.pictureUri = null
    }

    private fun initTopicSelectorButton() {
        topic.setOnClickListener {
            findNavController().navigate(
                if (currentMode == CreateCommunityMode.CREATE)
                    R.id.action_communityFragment_to_searchFragment
                else
                    R.id.action_communityCreateFragment_to_searchFragment,
                bundleOf("searchType" to SearchFragment.SEARCH_TYPE_GROUP_TOPICS)
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP) {
            val result = UCrop.getOutput(data!!)
            if (resultCode == Activity.RESULT_OK) {
                val uri = result
                model.pictureUri = uri
                coverPicture.setImageURI(uri)
                coverPicture.setVisibleOrGone(true)
                coverPictureButton.setVisibleOrGone(false)
                hideCoverPictureError()
            } else if (resultCode == UCrop.RESULT_ERROR) {
                val error = result
            }
        }

        if (resultCode == Activity.RESULT_OK) {
            when(requestCode){
                PickerUtils.PICK_IMAGE_CHOOSER_REQUEST_CODE -> {
                    val imageUri = PickerUtils.getPickImageResultUri(requireContext(), data)
                        openCoverCropper(imageUri!!)
                }
            }
            outputFile.delete()
            outputFile = CameraUtil.createFile(
                outputDirectory,
                FILENAME,
                PHOTO_EXTENSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            COMMUNITY_PERMISSIONS_REQUEST_CODE -> PickerUtils.startPickImageChooser(requireContext(), this)
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }

    private fun openCoverCropper(sourceUri: Uri) {
        UCrop.of(sourceUri, Uri.fromFile(outputFile))
            .withAspectRatio(21f, 7f)
            .start(requireContext(), this)
    }


}

interface CommunityCreateListener {

    fun onCommunityCreated()
    fun onCommunityChanged(group: Group)
    fun onCommunityDeleted()
}