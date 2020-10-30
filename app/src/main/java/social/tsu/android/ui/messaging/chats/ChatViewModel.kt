package social.tsu.android.ui.messaging.chats

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.models.PostUser
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.CreateMessagePayload
import social.tsu.android.network.model.Message
import social.tsu.android.service.ChatService
import social.tsu.android.service.ServiceCallback
import javax.inject.Inject


private const val TAG = "ChatViewModel"

class ChatViewModel @Inject constructor(
    private val application: TsuApplication,
    private val chatService: ChatService
) : ViewModel() {

    val isSending = ObservableBoolean()
    val messageText = ObservableField<String>()

    private val _errorLiveData = MutableLiveData<String>()
    val errorLiveData: LiveData<String> = _errorLiveData

    private val _recipient = MutableLiveData<PostUser>()
    val recipient: LiveData<PostUser> = _recipient

    val messagesLiveData: LiveData<PagedList<Message>> = Transformations.switchMap(_recipient) { recipient ->
        val recipientId = recipient?.id ?: run {
            Log.e(TAG, "Can't load messages. No selected recipient")
            return@switchMap MutableLiveData<PagedList<Message>>()
        }
        loadMessages(recipientId)
        chatService.getMessagesSource(recipientId)
    }

    private val pendingReadIds = hashSetOf<Int>()

    private fun loadMessages(recipientId: Int) {
        _errorLiveData.postValue(null)
        chatService.loadMessages(recipientId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
            }

            override fun onFailure(errMsg: String) {
                _errorLiveData.postValue(errMsg)
            }
        })
    }

    fun markAsRead(message: Message) {
        if (pendingReadIds.contains(message.id)) {
            return
        }
        pendingReadIds.add(message.id)
        chatService.markAsRead(message.id, object : ServiceCallback<Message> {
            override fun onSuccess(result: Message) {
                pendingReadIds.remove(message.id)
            }

            override fun onFailure(errMsg: String) {
                pendingReadIds.remove(message.id)
            }
        })
    }

    fun setRecipient(postUser: PostUser?) {
        _recipient.postValue(postUser)
    }

    fun sendMessage() {
        val senderId = AuthenticationHelper.currentUserId ?: run {
            Log.e(TAG, "Can't send message. No current user")
            return
        }

        val recipientId = _recipient.value?.id
        if (recipientId == null) {
            Log.e(TAG, "Can't send message. No selected recipient")
            return
        }

        val message = messageText.get()?.trim()
        if (message.isNullOrBlank()) return

        _errorLiveData.postValue(null)
        isSending.set(true)
        val payload = CreateMessagePayload(senderId, recipientId, message)
        chatService.sendMessage(payload, object : ServiceCallback<Message> {
            override fun onSuccess(result: Message) {
                isSending.set(false)
                messageText.set(null)
            }

            override fun onFailure(errMsg: String) {
                isSending.set(false)
                _errorLiveData.postValue(errMsg)
            }
        })
    }

}