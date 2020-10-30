package social.tsu.android.ui.live_stream

import androidx.lifecycle.ViewModel
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import social.tsu.android.data.repository.LiveStreamChatsRepository
import javax.inject.Inject

class LiveStreamViewModel @Inject constructor(
    private val liveStreamChatsRepo: LiveStreamChatsRepository
) : ViewModel() {

    val channelController by lazy {
        liveStreamChatsRepo.channelController
    }

    val myChatClient by lazy {
        liveStreamChatsRepo.chatsClient
    }

    private val watchingUsers = hashMapOf<String, User>()

    fun newUserWatching(user: User) {
        watchingUsers[user.id] = user
    }

    fun userStoppedWatching(user: User) {
        watchingUsers.remove(user.id)
    }

    fun getUsersWatchingLiveStream(): List<User> {
        return watchingUsers.values.toList()
    }

    fun initializeLiveChatsUser() = liveStreamChatsRepo.initializeChatsUser()

    fun banUser(messageUser: User) = liveStreamChatsRepo.banUser(messageUser)

    fun leave() = liveStreamChatsRepo.leave()

    fun join() = liveStreamChatsRepo.join()

    fun observeChatEvents() = liveStreamChatsRepo.observeChatEvents()

    fun deleteMessage(message: Message) = liveStreamChatsRepo.deleteMessage(message)

    fun flagMessage(message: Message) = liveStreamChatsRepo.flagMessage(message)

    fun muteUser(messageUser: User) = liveStreamChatsRepo.muteUser(messageUser)

    companion object {

        const val CHANNEL_ID = "tsulive"
        const val CHANNEL_TYPE = "livestream"
    }
}