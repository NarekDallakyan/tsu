package social.tsu.android.ui.messaging.chats

import android.Manifest
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import de.hdodenhof.circleimageview.CircleImageView
import social.tsu.android.R
import social.tsu.android.databinding.FragmentChatBinding
import social.tsu.android.helper.showUserProfile
import social.tsu.android.helper.Constants
import social.tsu.android.network.model.Message
import social.tsu.android.service.UserProfileImageService
import social.tsu.android.ui.*
import social.tsu.android.ui.new_post.PostDraftType
import social.tsu.android.ui.post_feed.main.MainFeedFragmentDirections
import social.tsu.android.ui.search.MENTION_TYPE
import social.tsu.android.ui.search.SearchFragment
import social.tsu.android.utils.*
import social.tsu.android.viewModel.MentionViewModel
import javax.inject.Inject

class ChatFragment : DaggerFragment() {

    @Inject
    lateinit var viewModel: ChatViewModel

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userProfileImageService: UserProfileImageService

    private val args: ChatFragmentArgs by navArgs()

    private var binding: FragmentChatBinding? = null

    private var toolbarView: View? = null
    private var recipientName: TextView? = null
    private var recipientPhoto: CircleImageView? = null

    private var mentionViewModel: MentionViewModel? = null

    private var initialText: String? = null


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
                        openMentionSearchFragment()
                        break
                    }
                }
            }

        }
    }

    private val adapter: ChatMessagesAdapter =
        ChatMessagesAdapter(object : ChatMessagesAdapter.Callback {
            override fun markAsRead(message: Message) {
                viewModel.markAsRead(message)
            }

            override fun didTapHashtag(hashtag: String) {
                findNavController().navigate(
                    R.id.hashtagGridFragment,
                    bundleOf("hashtag" to hashtag)
                )
            }

            override fun didTapUsername(username: String) {
                findNavController().showUserProfile(username)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setRecipient(args.recipient)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater)
        binding?.viewModel = viewModel
        binding?.chatTextInput?.setText("")


        mentionViewModel = ViewModelProvider(requireActivity()).get(MentionViewModel::class.java)
        toolbarView = inflater.inflate(R.layout.view_chat_header, null)
        toolbarView?.apply {
            recipientName = findViewById(R.id.chat_recipient_name)
            recipientPhoto = findViewById(R.id.chat_recipient_photo)
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.MATCH_PARENT
            )
        }
        binding?.chatTextInput?.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding?.chatTextInput?.addTextChangedListener(textWatcher)
                } else {
                    binding?.chatTextInput?.removeTextChangedListener(textWatcher)
                }
            }
        activity?.apply {
            findViewById<Toolbar>(R.id.toolbar)?.addView(toolbarView)
            setWindowResizeable()
        }

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.unbind()

        activity?.apply {
            findViewById<Toolbar>(R.id.toolbar)?.removeView(toolbarView)
            setWindowDefaultState()
            hideKeyboard()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.chatTextInput?.setText("")
        binding?.viewModel!!.messageText.set("")
        mentionViewModel?.selectTag("")
        binding?.unbind()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        args.recipient?.let { user ->
            val input = SpannableString(user.fullName.plus(" "))
            if (Constants.isVerified(user.verifiedStatus)) {
                val imageSpan = ImageSpan(requireContext(), R.drawable.ic_verified_small)
                input.setSpan(imageSpan, input.length - 1, input.length, 0)
            }
            recipientName?.text = input
            updateUserImages(user.profilePictureUrl)
        }

        val viewManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, true)
        viewManager.stackFromEnd = true
        binding?.chatMessagesList?.layoutManager = viewManager
        binding?.chatMessagesList?.adapter = adapter

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) snack(it)
        })
        viewModel.messagesLiveData.observe(viewLifecycleOwner, Observer {
            val position = viewManager.findFirstCompletelyVisibleItemPosition()
            adapter.submitList(it) {
                if (position <= 0) {
                    binding?.chatMessagesList?.scrollToPosition(0)
                }
            }
        })

        var lastFirstVisible = 0
        binding?.chatTextInput?.setOnClickListener {
            lastFirstVisible = viewManager.findFirstVisibleItemPosition()
        }
        binding?.chatTextInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (text.isNullOrEmpty()) {
                    binding?.chatSendMessage.hide()
                } else {
                    binding?.chatSendMessage.show()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        binding?.chatSendPhoto?.setOnClickListener(::onCameraClick)

        view.setOnKeyboardOpenListener({
            if (lastFirstVisible == 0) {
                binding?.chatMessagesList?.scrollToPosition(lastFirstVisible)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding?.viewModel = viewModel

        var tag = ""
        mentionViewModel?.getTag()?.observe(requireActivity(), Observer {
            if (!it.isNullOrEmpty()) {
                tag = it
            }
        })
        mentionViewModel?.selectTag("")
        if (!initialText.isNullOrEmpty()) {
            if (!tag.isNullOrEmpty()) {
                var finalText = initialText + tag
                binding?.viewModel!!.messageText.set(finalText)
                binding?.chatTextInput?.append(finalText)
            } else {
                binding?.viewModel!!.messageText.set(initialText)
                binding?.chatTextInput?.append(initialText)
            }
            binding?.chatTextInput?.post(Runnable {
                binding?.chatTextInput?.requestFocus()
                activity?.showKeyboard()
            })
        }

        initialText = ""
    }

    override fun onStart() {
        super.onStart()
        binding?.chatTextInput?.setText("")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chat_menu, menu)
    }

    /**
     * Open PostTypes Fragment with video option disabled as it is not allowed there
     */
    fun openPostTypesFragment() {
        dismissKeyboard()
        val direction = MainFeedFragmentDirections.showPostTypesFragment("")
        direction.allowVideo = false
        direction.popToDestination = R.id.chatFragment
        direction.recipient = viewModel.recipient.value
        direction.postingType = PostDraftType.MESSAGE
        findNavController().navigate(direction)
    }

    private fun updateUserImages(key: String?) {
        userProfileImageService.getProfilePicture(key, false) {
            adapter.otherUserDrawable = it
            it?.let {
                recipientPhoto?.setImageDrawable(it)
            } ?: run {
                recipientPhoto?.setImageResource(R.drawable.user)
            }
        }
    }

    private fun onCameraClick(view: View) {
        checkPermissions(
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ),
            MainActivity.NEW_MESSAGE_PERMISSIONS_REQUEST_CODE
        ) {
            openPostTypesFragment()
        }
    }

    private fun openMentionSearchFragment() {
        dismissKeyboard()
        initialText = binding?.chatTextInput?.text.toString()
        binding?.chatTextInput?.isSelected = false
        binding?.chatTextInput?.isFocusable = false

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.chatFragment) {
            navController.navigate(
                R.id.action_chatFragment_To_mentionSearchFragment,
                bundleOf(
                    "searchType" to SearchFragment.SEARCH_TYPE_MENTION,
                    MENTION_TYPE to SearchFragment.MENTION_TYPE_CHAT
                )
            )
        }
    }

}