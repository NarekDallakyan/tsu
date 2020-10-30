package social.tsu.android.ui.post_feed.edit_post

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.compose_post.*
import social.tsu.android.R
import social.tsu.android.adapters.viewholders.PostViewHolder
import social.tsu.android.network.api.HostProvider
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.search.MENTION_TYPE
import social.tsu.android.ui.search.SearchFragment
import social.tsu.android.ui.showKeyboard
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.MentionViewModel
import javax.inject.Inject

class EditPostFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<EditPostViewModel> { viewModelFactory }

    private val args: EditPostFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    private var mentionViewModel: MentionViewModel? = null
    var composeEdit: EditText? = null

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

            var value = arg0.toString()
            if (value.isNullOrEmpty()) {
                return
            }

            if (value.contains("@")) {
                val split = value.split(" ")
                for (item in split) {
                    if (item.contains("@") && item.length == 1) {
                        initialText = composeEdit?.text.toString()
                        openMentionSearchFragment()
                        break
                    }
                }
            }

        }
    }


    private fun openMentionSearchFragment() {
        dismissKeyboard()
        initialText = composeEdit?.text.toString()
        composeEdit?.isSelected = false
        composeEdit?.isFocusable = false


        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.editPostFragment) {
            navController.navigate(
                R.id.action_postEdit_to_mentionSearchFragment,
                bundleOf(
                    "searchType" to SearchFragment.SEARCH_TYPE_MENTION,
                    MENTION_TYPE to SearchFragment.MENTION_TYPE_EDIT_POST
                )
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.draft_post, container, false)

        composeEdit = view.findViewById(R.id.composePost) as EditText

        mentionViewModel = ViewModelProvider(requireActivity()).get(MentionViewModel::class.java)

        view.findViewById<View>(R.id.btn_add_photo).visibility = View.GONE
        view.findViewById<View>(R.id.btn_add_video).visibility = View.GONE

        viewModel.postId = args.postId
        composeEdit?.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    composeEdit?.addTextChangedListener(textWatcher)
                } else {
                    composeEdit?.removeTextChangedListener(textWatcher)
                }
            }
        setHasOptionsMenu(true)
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        composeEdit?.setText("")
        mentionViewModel?.selectTag("")
    }


    override fun onStart() {
        super.onStart()
        composeEdit?.setText("")
    }


    override fun onResume() {
        super.onResume()

        var tag = ""
        composeEdit?.setText("")
        mentionViewModel?.getTag()?.observe(requireActivity(), Observer {
            if (!it.isNullOrEmpty()) tag = it
        })
        mentionViewModel?.selectTag("")
        if (!initialText.isNullOrEmpty()) {
            if (!tag.isNullOrEmpty()) {

                composeEdit?.append(initialText + tag)
            } else {
                composeEdit?.append(initialText)
            }
        }

        composeEdit?.post(Runnable {
            composeEdit?.requestFocus()
            activity?.showKeyboard()
        })
        initialText = ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.post.observe(viewLifecycleOwner, Observer {
            it?.let {
                composeEdit?.setText(it.content)
                composeEdit?.setSelection(composeEdit?.text?.length!!)
                composeEdit?.requestFocus()
                val imageView = view.findViewById<ImageView>(R.id.imgPreview)
                when {
                    it.has_picture -> {
                        Glide
                            .with(view.context as Context)
                            .load(formatUrl(it.picture_url))
                            .thumbnail(PostViewHolder.POST_IMAGE_THUMBNAIL_QUALITY)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView)
                    }
                    it.stream != null -> {
                        Glide
                            .with(view.context as Context)
                            .load(streamFormatURL(it.stream.thumbnail))
                            .thumbnail(PostViewHolder.POST_IMAGE_THUMBNAIL_QUALITY)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView)
                    }
                    else -> {
                        // really kotlin ? really ???
                    }
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save_btn -> {
                viewModel.post.observe(viewLifecycleOwner, Observer {
                    if (!it.has_picture && it.stream == null && view?.findViewById<EditText>(R.id.composePost)?.text.isNullOrBlank()) {
                        dismissKeyboard()
                        Toast.makeText(
                            context,
                            getString(R.string.enter_text_message),
                            Toast.LENGTH_LONG
                        ).show()
                        progress_bar.visibility = View.GONE
                    } else {
                        composeEdit?.text?.let {
                            dismissKeyboard()
                            if (requireActivity().isInternetAvailable()) {
                                viewModel.savePostEdits(it.toString())
                                    .observe(viewLifecycleOwner, Observer {
                                        when (it) {
                                            is Data.Success -> findNavController().navigateUp()
                                            is Data.Loading -> {

                                            }
                                            is Data.Error -> it.throwable.message?.let { message ->
                                                snack(message)
                                            }
                                        }
                                    })
                            } else
                                requireActivity().internetSnack()
                        }
                    }
                })
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        dismissKeyboard()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
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


}