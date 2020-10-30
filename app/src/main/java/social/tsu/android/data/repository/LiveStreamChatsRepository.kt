package social.tsu.android.data.repository


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.controllers.ChannelController
import io.getstream.chat.android.client.events.ChatEvent
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.observable.Subscription
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.models.ChatUserData
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.runIfUserIsAuthenticated
import social.tsu.android.network.model.ChatTokenResponse
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.*
import social.tsu.android.ui.live_stream.LiveStreamViewModel
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.sanitizeUserName
import javax.inject.Inject

class LiveStreamChatsRepository @Inject constructor(
    private val application: TsuApplication,
    private val authenticationService: AuthenticationService,
    private val analyticsHelper: AnalyticsHelper
) : UserInfoServiceCallback {

    private var hasJoinedChannel = false
    private var hasLeftChannel = false

    companion object {

        private val TAG = LiveStreamChatsRepository::class.java.simpleName

    }

    val chatsClient by lazy {
        Log.d(TAG, "create chatsClient")
        analyticsHelper.log("create chatsClient")
        ChatClient.instance()
    }

    val channelController: ChannelController by lazy {
        Log.d(TAG, "create channelController")
        analyticsHelper.log("create channelController")
        chatsClient.channel(LiveStreamViewModel.CHANNEL_TYPE, LiveStreamViewModel.CHANNEL_ID)
    }

    private val userInfoService: UserInfoService by lazy {
        DefaultUserInfoService(application, this)
    }

    private val _loadState = MutableLiveData<Data<Boolean>>()
    val loadState: LiveData<Data<Boolean>> = _loadState

    fun initializeChatsUser(): LiveData<Data<Boolean>> {
        Log.d(TAG, "initializeChatsUser")
        analyticsHelper.log("initializeChatsUser")
        _loadState.value = Data.Loading()
        try {
            if (ChatClient.instance().getCurrentUser() != null) {
                _loadState.value = Data.Success(true)
                return loadState
            }
        } catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
        }
        runIfUserIsAuthenticated {
            userInfoService.getUserInfo(it, true)
        }
        return loadState
    }

    private fun generateChatsToken(chatUserData: ChatUserData) {
        Log.d(TAG, "generateChatsToken")
        analyticsHelper.log("generateChatsToken")

        authenticationService.generateChatsToken(chatUserData.username, object : ServiceCallback<ChatTokenResponse> {
            override fun onSuccess(result: ChatTokenResponse) {

                AuthenticationHelper.setupLiveChatsUser(
                    application,
                    chatUserData,
                    result.token,
                    onSuccess = {
                        _loadState.value = Data.Success(true)
                    },
                    onFailure = {
                        _loadState.value = Data.Error(Throwable(it))
                    }
                )
            }

            override fun onFailure(errMsg: String) {
                _loadState.value = Data.Error(Throwable(errMsg))
            }
        })

    }

    override fun completedGetUserInfo(info: UserProfile?) {
        info?.let { tsuUser ->
            val sanitizedUsername = sanitizeUserName(tsuUser.username)
            val chatUserData = ChatUserData(
                tsuUser.id,
                sanitizedUsername,
                tsuUser.fullName,
                tsuUser.profilePictureUrl,
                tsuUser.verifiedStatus
            )
            generateChatsToken(chatUserData)
        }
    }

    override fun didErrorWith(message: String) {
        _loadState.value = Data.Error(Throwable(message))
    }

    fun banUser(messageUser: User): LiveData<Data<Boolean>> {
        val banLoadState = MutableLiveData<Data<Boolean>>()

        banLoadState.value = Data.Loading()
        channelController.banUser(
            messageUser.id,
            "",
            Int.MAX_VALUE
        ).enqueue {
            if (it.isSuccess) {
                banLoadState.value = Data.Success(true)
                Log.d(TAG, "User ${messageUser.id} banned")
            } else {
                banLoadState.value = Data.Error(it.error())
                Log.e(
                    TAG,
                    "Banning user ${messageUser.id} failed with error: ${it.error().message}"
                )
            }
        }
        return banLoadState
    }

    fun muteUser(messageUser: User): LiveData<Data<Boolean>> {
        val muteLoadState = MutableLiveData<Data<Boolean>>()

        muteLoadState.value = Data.Loading()
        chatsClient.muteUser(
            messageUser.id
        ).enqueue {
            if (it.isSuccess) {
                muteLoadState.value = Data.Success(true)
                Log.d(TAG, "User ${messageUser.id} muted")
            } else {
                muteLoadState.value = Data.Error(it.error())
                Log.e(
                    TAG,
                    "Muting user ${messageUser.id} failed with error: ${it.error().message}"
                )
            }
        }
        return muteLoadState
    }


    fun flagMessage(message: Message): LiveData<Data<Boolean>> {
        val flagLoadState = MutableLiveData<Data<Boolean>>()

        flagLoadState.value = Data.Loading()
        chatsClient.flag(message.id).enqueue {
            if (it.isSuccess) {
                flagLoadState.value = Data.Success(true)
                Log.d(TAG, "Message ${message.id} flagged")
            } else {
                flagLoadState.value = Data.Error(it.error())
                Log.e(
                    TAG,
                    "Flagging user ${message.id} failed with error: ${it.error().message}"
                )
            }
        }

        return flagLoadState
    }


    fun deleteMessage(message: Message): LiveData<Data<Boolean>> {
        val deleteLoadState = MutableLiveData<Data<Boolean>>()

        deleteLoadState.value = Data.Loading()
        chatsClient.deleteMessage(message.id).enqueue {
            if (it.isSuccess) {
                Log.d(TAG, "Message ${message.id} deleted")
                deleteLoadState.value = Data.Success(true)
            } else {
                deleteLoadState.value = Data.Error(it.error())
                Log.e(
                    TAG,
                    "Deleting user ${message.id} failed with error: ${it.error().message}"
                )
            }
        }

        return deleteLoadState

    }

    private var chatEventsSubscription: Subscription? = null
    fun join(): LiveData<Data<Boolean>> {

        val watchLoadState = MutableLiveData<Data<Boolean>>()
        if (hasJoinedChannel) return watchLoadState

        Log.d(TAG, "try to join channel")
        analyticsHelper.log("try to join channel")

        watchLoadState.value = Data.Loading()
        channelController.watch().enqueue {
            if (it.isSuccess) {
                Log.d(TAG, "join channel succeeded")
                analyticsHelper.log("join channel succeeded")
                hasJoinedChannel = true
                watchLoadState.value = Data.Success(true)
                channelController.addMembers(chatsClient.getCurrentUser()?.id ?: "").enqueue {}
                //sendUserJoinedStreamMessage()
            } else {
                Log.d(TAG, "join channel failed")
                analyticsHelper.log("join channel failed")
                watchLoadState.value = Data.Error(it.error())
            }
        }
        return watchLoadState
    }

    fun observeChatEvents(): LiveData<ChatEvent> {
        Log.d(TAG, "observeChatEvents")
        analyticsHelper.log("observeChatEvents")
        val chatEventsLD = MutableLiveData<ChatEvent>()
        try {
            chatEventsSubscription = channelController.events().subscribe {
                chatEventsLD.value = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return chatEventsLD
    }


    private fun sendUserLeftStreamMessage() {
        if (hasLeftChannel) return
        val name = chatsClient.getCurrentUser()?.extraData?.get(ChatUserData.NAME_KEY) as? String
        name?.let { fullname ->
            val leftMessage = Message().apply {
                text = application.getString(R.string.left_stream_message, fullname)
            }
            channelController.sendMessage(leftMessage).enqueue {
                if (it.isSuccess) {
                    hasLeftChannel = true
                    Log.d(TAG, "Send user left stream message success")
                } else {
                    Log.e(
                        TAG,
                        "Sending left stream message failed with error: ${it.error().message}"
                    )
                }
            }
        }
    }

    fun leave() {
        hasJoinedChannel = false
        hasLeftChannel = false
        //TODO: Enable it when you need to show a message that a user has left the stream
        // sendUserLeftStreamMessage()
        chatEventsSubscription?.unsubscribe()
        try {
            channelController.stopWatching().enqueue { }
        } catch (e: UninitializedPropertyAccessException) {
            Log.w(TAG, "ChatClient is not initialized. Can't leave channel")
        }
    }
}