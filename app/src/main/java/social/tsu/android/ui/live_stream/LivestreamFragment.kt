package social.tsu.android.ui.live_stream

import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.getstream.sdk.chat.viewmodel.ChannelViewModel
import com.getstream.sdk.chat.viewmodel.ChannelViewModelFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioListener
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.vdurmont.emoji.EmojiManager
import io.getstream.chat.android.client.events.MessageDeletedEvent
import io.getstream.chat.android.client.events.NewMessageEvent
import io.getstream.chat.android.client.events.UserStartWatchingEvent
import io.getstream.chat.android.client.events.UserStopWatchingEvent
import io.getstream.chat.android.client.models.Message
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import org.json.JSONObject
import retrofit2.Response
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.models.ChatMessageData
import social.tsu.android.data.local.models.ChatUserType
import social.tsu.android.databinding.DialogBottomLiveStreamChatActionsBinding
import social.tsu.android.databinding.DialogBottomLiveStreamMyMessageActionsBinding
import social.tsu.android.databinding.FragmentLivestreamBinding
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.api.HlsResponse
import social.tsu.android.network.api.LiveApi
import social.tsu.android.network.model.DacastResponse
import social.tsu.android.network.model.HLSStream
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.FlyEmojiAnimation
import social.tsu.android.utils.*
import java.net.UnknownHostException
import javax.inject.Inject

class LivestreamFragment : Fragment(),
    LiveStreamChatsAdapter.LiveStreamMessageContentCallback {

    enum class MediaType {
        LIVE, VOD
    }

    private var disposable: Disposable? = null

    @Inject
    lateinit var liveApi: LiveApi

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper
    private var isLive: Boolean = false
    val properties = HashMap<String, Any?>()

    private val gson: Gson = Gson()

    private var player: SimpleExoPlayer? = null
    private lateinit var playerView: PlayerView

    private lateinit var volumeToggle: ImageButton
    private lateinit var playerProgressBar: ProgressBar

    private var currentUrl = ""
    private var currentMediaType = MediaType.LIVE
    private var startOffset = 0L
    private var volume: Float = 0f

    private lateinit var binding: FragmentLivestreamBinding
    private lateinit var chatsChannelViewModel: ChannelViewModel

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val liveStreamChatsViewModel by viewModels<LiveStreamViewModel> { viewModelFactory }

    private val liveStreamChatsAdapter by lazy {
        LiveStreamChatsAdapter(
            requireContext(),
            liveStreamChatsViewModel.myChatClient.getCurrentUser()!!,
            this
        )
    }

    companion object {
        private const val KEY_CURRENT_TYPE = "current_type"
        private const val KEY_CURRENT_URL = "current_url"
        private const val KEY_SEEK_POS = "seek_pos"
        private const val KEY_VOLUME = "volume"
        private val TAG = LivestreamFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentComponent =
            (activity?.application as TsuApplication).appComponent.fragmentComponent().create()

        fragmentComponent.inject(this)
        setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLivestreamBinding.inflate(inflater)

        player = requireContext().createAppExoPlayer()

        playerView = binding.playerView
        binding.closeButton.setOnClickListener {
            if (isLive) {
                properties["VOD"] = false
                isLive = false
            } else {
                properties["VOD"] = true
            }
            analyticsHelper.logEvent("tsu_live_stop_view", properties)
            findNavController().popBackStack()
        }
        volumeToggle = playerView.findViewById(R.id.control_mute)
        playerProgressBar = binding.liveStreamPlayerProgress

        playerView.player = player

        volumeToggle.setOnClickListener(::toggleVolume)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUrl = savedInstanceState?.getString(KEY_CURRENT_URL) ?: ""
        currentMediaType = MediaType.valueOf(savedInstanceState?.getString(KEY_CURRENT_TYPE) ?: currentMediaType.name)
        startOffset = savedInstanceState?.getLong(KEY_SEEK_POS) ?: 0
        volume = savedInstanceState?.getFloat(KEY_VOLUME) ?: 0f

        if (currentUrl.isEmpty()) {
            prepareNextVideo()
        } else {
            startVideo()
        }
    }

    private fun setupLiveChats() {
        liveStreamChatsViewModel.initializeLiveChatsUser().observe(viewLifecycleOwner, Observer {
            when (it) {
                is Data.Loading -> {
                    binding.liveStreamChatsSetupProgress.show()
                }
                is Data.Success -> {
                    setupChatsUi()
                    binding.liveStreamChatsSetupProgress.hide()
                }
                is Data.Error -> {
                    if ((it is UnknownHostException)) {
                        snack(requireContext().getString(R.string.connectivity_issues_message))
                    } else {
                        it.throwable.message?.let { msg -> snack(msg) }
                    }
                    binding.liveStreamChatsSetupProgress.hide()
                }
            }
        })
    }

    private fun updateChatOrientationLayout() {
        val currentOrientation = resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.liveStreamChats.root.hide()
        } else {
            if (isChatsClientInitialized) binding.liveStreamChats.root.show()
        }
    }

    private var isChatsClientInitialized = false
    private fun setupChatsUi() {
        Log.d(TAG, "setupChatsUi")
        analyticsHelper.log("setupChatsUi")
        isChatsClientInitialized = true
        updateChatOrientationLayout()

        binding.liveStreamChats.messageInput.setupEmojiList(binding.emojiFlightContainer)

        val viewModelFactory = ChannelViewModelFactory(
            requireContext().applicationContext as Application,
            LiveStreamViewModel.CHANNEL_TYPE,
            LiveStreamViewModel.CHANNEL_ID
        )
        chatsChannelViewModel =
            ViewModelProvider(this, viewModelFactory).get(ChannelViewModel::class.java)

        binding.viewModel = chatsChannelViewModel


        chatsChannelViewModel.initialized.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "chatsChannelViewModel_initialized")
            analyticsHelper.log("chatsChannelViewModel_initialized")
            // connect the view model
            joinChats()
            observeChatEvents()

            binding.viewModel = chatsChannelViewModel
            binding.lifecycleOwner = viewLifecycleOwner

            binding.liveStreamChats.messageInput.setViewModel(
                chatsChannelViewModel,
                viewLifecycleOwner,
                liveStreamChatsViewModel.channelController
            )
            (binding.liveStreamChats.messageList.layoutManager as LinearLayoutManager).stackFromEnd =
                true
            binding.liveStreamChats.messageList.adapter = liveStreamChatsAdapter

            binding.liveStreamWatchersLayout?.setOnClickListener {
                // TODO:go to watchers list
            }
        }
        )
    }

    private fun joinChats() {
        liveStreamChatsViewModel.join().observe(viewLifecycleOwner, Observer {
            when (it) {
                is Data.Success -> {
                    liveStreamChatsViewModel.myChatClient.getCurrentUser()?.let { user ->
                        liveStreamChatsViewModel.newUserWatching(user)
                        liveStreamChatsAdapter.newUserJoined(user)
                    }
                }
                is Data.Error -> {
                }
            }
        })

        observeChatEvents()
    }

    private fun observeChatEvents() {
        liveStreamChatsViewModel.observeChatEvents().observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "new chats with type: $it")
            when (it) {
                is NewMessageEvent -> {
                    if (isReaction(it.message)) {
                        showReaction(it.message)
                    } else {
                        liveStreamChatsAdapter.addMessage(it.message)
                    }
                }
                is MessageDeletedEvent -> {
                    liveStreamChatsAdapter.deleteMessage(it.message)
                }
                is UserStartWatchingEvent -> {
                    it.user?.let { user ->
                        liveStreamChatsViewModel.newUserWatching(user)
                        liveStreamChatsAdapter.newUserJoined(user)
                    }
                }
                is UserStopWatchingEvent -> {
                    it.user?.let { user ->
                        liveStreamChatsViewModel.userStoppedWatching(user)
                    }
                }
            }
        })
    }


    override fun onResume() {
        super.onResume()
        playerView.onResume()
        player?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        playerView.onPause()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        super.onDestroy()
        releasePlayer()
        disposable?.dispose()
        isLive = false
        liveStreamChatsViewModel.leave()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (currentUrl.isNotEmpty()) {
            outState.putString(KEY_CURRENT_TYPE, currentMediaType.name)
            outState.putString(KEY_CURRENT_URL, currentUrl)
            outState.putLong(KEY_SEEK_POS, player?.contentPosition ?: 0)
            outState.putFloat(KEY_VOLUME, player?.volume ?: 0f)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateChatOrientationLayout()
    }

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
    }

    private fun generateMediaSource(): MediaSource {
        val defaultDataSourceFactory = DefaultDataSourceFactory(requireContext(), "tsu-android")
        return HlsMediaSource.Factory(defaultDataSourceFactory)
            .setTag(currentUrl)
            .setAllowChunklessPreparation(true)
            .createMediaSource(Uri.parse(currentUrl))
    }

    private fun prepareNextVideo(switchToNextType: Boolean = false) {
        startOffset = 0
        if (switchToNextType) {
            if (currentMediaType == MediaType.LIVE) {
                currentMediaType = MediaType.VOD
            } else {
                currentMediaType = MediaType.LIVE
            }
        }
        disposable?.let {
            if (it.isDisposed.not())
                it.dispose()
        }

        when (currentMediaType) {
            MediaType.LIVE -> {
                disposable = liveApi.getDacastToken().subscribeOn(schedulers.io())
                    .onErrorReturn {
                        return@onErrorReturn DacastResponse(
                            505,
                            requireContext().getString(R.string.connectivity_issues_message)
                        )
                    }
                    .map { it.token }
                    .zipWith(
                        liveApi.getDacast().subscribeOn(schedulers.io()).map {
                            val jsonString = it.string()
                            val json = JSONObject(jsonString)
                            val packages = json.getString("package").split(",")
                            val streams = ArrayList<String>()
                            for (name in packages) {
                                val hlsStream = gson.fromJson(
                                    json.getJSONObject(name).toString(),
                                    HLSStream::class.java
                                )
                                streams.add(hlsStream.hls)
                            }
                            streams
                        },
                        BiFunction<String, ArrayList<String>, List<String>> { t1, t2 -> t2.map { it + t1 } })
                    .observeOn(schedulers.main())
                    .subscribe({
                        if (it.isNotEmpty()) {
                            currentUrl = it.first()
                            startVideo()
                        }
                    }, {
                        it?.let {
                            if ((it is UnknownHostException)) {
                                snack(requireContext().getString(R.string.connectivity_issues_message))
                            } else {
                                it.message?.let { it1 -> snack(it1) }
                                currentMediaType = MediaType.VOD
                                prepareNextVideo()
                            }
                        }
                    })

            }
            MediaType.VOD -> {
                disposable = liveApi.getPlaylist().flatMap {
                    var result: Single<Response<HlsResponse>> = Single.never()
                    handleResponse(requireContext(), it,
                        onSuccess = {
                            startOffset = parseOffset(it.data.offset)
                            result = liveApi.getPlaybackHls(it.data.hls)
                        },
                        onFailure = {
                            currentMediaType = MediaType.LIVE
                            if (requireActivity().isInternetAvailable())
                                prepareNextVideo()
                        })

                    return@flatMap result
                }.observeOn(schedulers.main())
                    .subscribeOn(schedulers.io())
                    .subscribe({
                        handleResponse(requireContext(), it,
                            onSuccess = {
                                currentUrl = it.hls
                                startVideo()
                            },
                            onFailure = {
                                currentMediaType = MediaType.LIVE
                                if (requireActivity().isInternetAvailable())
                                    prepareNextVideo()
                            })
                    }, {
                        currentMediaType = MediaType.LIVE
                        it?.let {
                            if (it is UnknownHostException) {
                                snack(requireContext().getString(R.string.connectivity_issues_message))
                            } else
                                prepareNextVideo()
                        }
                    })
            }
        }
    }

    private fun parseOffset(offset: String): Long {
        val trimmedOffset = offset.trimStart('-')
        return trimmedOffset.split(":").reversed().map { it.toInt() }.mapIndexed { index, value ->
                if (index == 0) {
                    return@mapIndexed value * 1000 //seconds
                } else if (index == 1) {
                    return@mapIndexed value * 1000 * 60 //minutes
                } else {
                    return@mapIndexed value * 1000 * 60 * 60 //hours
                }
            }.sum().toLong()
    }

    private fun startVideo() {
        analyticsHelper.log("startLive")
        player?.prepare(generateMediaSource())
        player?.playWhenReady = true
        player?.seekTo(startOffset)
        player?.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                if (requireActivity().isInternetAvailable())
                    prepareNextVideo(switchToNextType = true)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    if (requireActivity().isInternetAvailable())
                        prepareNextVideo(switchToNextType = true)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    playerProgressBar.hide()
                    val duration = player?.duration ?: 0
                    Log.d(
                        "livetest:",
                        "it is:" + player?.isCurrentWindowLive + player?.isCurrentWindowDynamic + player?.isCurrentWindowSeekable
                    )
                    try {
                        if (player?.isCurrentWindowLive!! || player?.isCurrentWindowDynamic!!) {
                            Log.d(TAG, "setupLiveChats ready to start")
                            isLive = true
                            properties["VOD"] = false
                            analyticsHelper.log("setupLiveChats ready to start")
                            lifecycleScope.launchWhenResumed { setupLiveChats() }
                        } else {
                            properties["VOD"] = true
                        }
                        analyticsHelper.logEvent("tsu_live_start_view", properties)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    playerProgressBar.show()
                }
            }
        })
        player?.addAudioListener(object : AudioListener {
            override fun onVolumeChanged(volume: Float) {
                updateVolumeUI(volume)
            }
        })
        player?.volume = volume
        updateVolumeUI(volume)
    }

    private fun toggleVolume(view: View) {
        player?.volume = if (player?.volume == 0.0f) {
            1.0f
        } else {
            0.0f
        }
    }

    private fun updateVolumeUI(volume: Float) {
        if (volume == 0.0f) {
            volumeToggle.setImageResource(R.drawable.ic_mute_on)
        } else {
            volumeToggle.setImageResource(R.drawable.ic_mute_off)
        }
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

    private fun showReaction(message: Message) {
        FlyEmojiAnimation(requireContext(), binding.emojiFlightContainer).flyEmoji(
            message.text
        )
    }

    override fun onUserTapped(userId: Int) {
        findNavController().showUserProfile(userId)
    }

    override fun onIncomingMessageLongPress(message: Message) {
        showChatModerationBottomSheet(message)
    }

    override fun onOutgoingMessageLongPress(message: Message) {
        showMyMessageBottomSheet(message)
    }

    private fun showChatModerationBottomSheet(messageTapped: Message) {
        val messageUser = messageTapped.user

        val sheetBinding = DialogBottomLiveStreamChatActionsBinding.inflate(layoutInflater)
        sheetBinding.viewModel = chatsChannelViewModel
        val actionSheet = BottomSheetDialog(context as Context)
        actionSheet.setContentView(sheetBinding.root)

        actionSheet.show()

        val currentUser = liveStreamChatsViewModel.myChatClient.getCurrentUser()
        if (currentUser?.role == ChatUserType.ADMIN.key ||
            currentUser?.role == ChatUserType.SHERIFF.key
        ) {
            sheetBinding.dialogBanButton.show()
            sheetBinding.banMuteOptionsDivider.show()
        }

        sheetBinding.dialogBanButton.setOnClickListener {
            liveStreamChatsViewModel.banUser(messageUser)
            actionSheet.dismiss()
        }

        sheetBinding.dialogCancelButton.setOnClickListener {
            actionSheet.dismiss()
        }

        sheetBinding.dialogFlagButton.setOnClickListener {
            liveStreamChatsViewModel.flagMessage(messageTapped)
            actionSheet.dismiss()
        }

        sheetBinding.dialogMuteButton.setOnClickListener {
            liveStreamChatsViewModel.muteUser(messageUser)
            actionSheet.dismiss()
        }
    }


    private fun showMyMessageBottomSheet(messageTapped: Message) {
        if (messageTapped.deletedAt != null) {
            return
        }

        val sheetBinding = DialogBottomLiveStreamMyMessageActionsBinding.inflate(layoutInflater)
        sheetBinding.viewModel = chatsChannelViewModel
        val actionSheet = BottomSheetDialog(context as Context)
        actionSheet.setContentView(sheetBinding.root)

        actionSheet.show()

        sheetBinding.dialogDeleteButton.setOnClickListener {
            liveStreamChatsViewModel.deleteMessage(messageTapped)
            actionSheet.dismiss()
        }

        sheetBinding.dialogCancelButton.setOnClickListener {
            actionSheet.dismiss()
        }
    }

    private fun isReaction(message: Message?): Boolean {
        if (message == null) return false
        return (message.extraData[ChatMessageData.IS_REACTION_KEY] as? Boolean)
            ?: (EmojiManager.containsEmoji(message.text) && !message.text.toCharArray()
                .any { it.isLetterOrDigit() })
    }


}