package social.tsu.android.ui.post_feed.edit_post

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.Coil
import coil.api.load
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.compose_post.progress_bar
import kotlinx.android.synthetic.main.draft_post.*
import social.tsu.android.R
import social.tsu.android.network.api.HostProvider
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.post.helper.PostTypeDraftUiHelper
import social.tsu.android.utils.findParentNavController
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.MentionViewModel
import social.tsu.android.viewModel.SharedViewModel
import javax.inject.Inject

class EditPostFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<EditPostViewModel> { viewModelFactory }
    private var sharedViewModel: SharedViewModel? = null

    private val args: EditPostFragmentArgs by navArgs()

    // Ui handler
    private val draftUiHandler = PostTypeDraftUiHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    private var mentionViewModel: MentionViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.draft_post, container, false)
        viewModel.postId = args.postId
        return view
    }

    private fun initViewModel() {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        mentionViewModel = ViewModelProvider(requireActivity()).get(MentionViewModel::class.java)
    }

    private fun initUi() {

        postVisibility.hide()
        saveDeviceLayout.hide()
        postDraftTitle.text = "Edit Post"
        actionText.text = "Save"

        // handle description close visibility
        draftUiHandler.handleDescriptionUi(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        descriptionEditText?.setText("")
        mentionViewModel?.selectTag("")
    }


    override fun onStart() {
        super.onStart()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()
        descriptionEditText?.setText("")
    }


    override fun onResume() {
        super.onResume()

        var tag = ""
        descriptionEditText?.setText("")
        mentionViewModel?.getTag()?.observe(requireActivity(), Observer {
            if (!it.isNullOrEmpty()) tag = it
        })
        mentionViewModel?.selectTag("")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // init view model
        initViewModel()
        // init on clicks
        initOnClicks()
        // init ui
        initUi()
        // get post
        getPost()
    }

    private fun getPost() {

        viewModel.post.observe(viewLifecycleOwner, Observer {
            it?.let {
                descriptionEditText?.setText(it.content)
                descriptionEditText?.setSelection(descriptionEditText?.text?.length!!)
                descriptionEditText?.requestFocus()
                val imageView = view?.findViewById<ImageView>(R.id.imgPreview)
                when {
                    it.has_picture -> {
                        return@let Coil.load(requireContext(), formatUrl(it.picture_url)) {
                            target { fileDrawable ->
                                imageView?.setImageDrawable(fileDrawable)
                            }
                        }
                    }
                    it.stream != null -> {
                        return@let Coil.load(
                            requireContext(),
                            streamFormatURL(it.stream.thumbnail)
                        ) {
                            target { fileDrawable ->
                                imageView?.setImageDrawable(fileDrawable)
                            }
                        }
                    }
                    else -> {
                        // really kotlin ? really ???
                    }
                }
            }
        })
    }

    private fun initOnClicks() {

        // Listen save button clicked
        postButton?.setOnClickListener {

            onSave()
        }

        // Listen back button clicked
        closePostDraft?.setOnClickListener {

            sharedViewModel?.select(false)
            findParentNavController().popBackStack(R.id.mainFeedFragment, false)
        }
    }

    override fun onDestroyView() {
        dismissKeyboard()
        super.onDestroyView()
    }

    private fun dismissKeyboard() {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun formatUrl(url: String): String {
        // Handle relative paths from the API_HOST
        if (url.startsWith("/")) {
            return "${HostProvider.imageHost}${url}"
        }

        return url
    }

    private fun streamFormatURL(url: String): String {
        // Handle relative paths from the API_HOST
        if (url.startsWith("/")) {
            return "${HostProvider.videoHost}${url}"
        }

        return url
    }


    private fun onSave() {

        progress_bar.show()
        viewModel.post.observe(viewLifecycleOwner, Observer {
            if (!it.has_picture && it.stream == null && view?.findViewById<EditText>(R.id.composePost)?.text.isNullOrBlank()) {
                dismissKeyboard()
                Toast.makeText(
                    context,
                    getString(R.string.enter_text_message),
                    Toast.LENGTH_LONG
                ).show()
                progress_bar.hide()
            } else {

                dismissKeyboard()

                val changedText = descriptionEditText?.text?.toString()

                if (changedText == null) {
                    progress_bar.hide()
                    return@Observer
                }

                if (!requireActivity().isInternetAvailable()) {
                    progress_bar.hide()
                    requireActivity().internetSnack()
                    return@Observer
                }

                viewModel.savePostEdits(changedText)
                    .observe(viewLifecycleOwner, Observer {
                        when (it) {
                            is Data.Success -> findNavController().navigateUp()
                            is Data.Loading -> {

                            }
                            is Data.Error -> it.throwable.message?.let { message ->
                                progress_bar.hide()
                                snack(message)
                            }
                        }
                    })
            }
        })
    }
}