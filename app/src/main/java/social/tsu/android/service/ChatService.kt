package social.tsu.android.service

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.dao.MessagingDao
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.network.api.MessagingApi
import social.tsu.android.network.model.CreateMessageDTO
import social.tsu.android.network.model.CreateMessagePayload
import social.tsu.android.network.model.MarkAsReadDTO
import social.tsu.android.network.model.Message
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


abstract class ChatService : DefaultService() {

    abstract fun sendMessage(
        createMessagePayload: CreateMessagePayload,
        callback: ServiceCallback<Message>
    )

    abstract fun loadMessages(recipientId: Int, callback: ServiceCallback<Boolean>)

    abstract fun loadPreviousMessages(recipientId: Int, lastMessageId: Int)

    abstract fun loadNewMessages(recipientId: Int)

    abstract fun getMessagesSource(recipientId: Int): LiveData<PagedList<Message>>

    abstract fun getRecentContact(recipientId: Int): LiveData<RecentContact>

    abstract fun markAsRead(messageId: Int, callback: ServiceCallback<Message>)
}

private const val DEFAULT_PAGE_LIMIT = 15

class DefaultChatService @Inject constructor(
    private val application: TsuApplication,
    private val messagingApi: MessagingApi,
    private val schedulers: RxSchedulers,
    private val messagingDao: MessagingDao,
    private val moshi: Moshi
) : ChatService() {

    override val tag: String = "DefaultResetPasswordService"

    override val compositeDisposable = CompositeDisposable()

    private val config: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(DEFAULT_PAGE_LIMIT)
        .setEnablePlaceholders(true)
        .build()

    override fun getRecentContact(recipientId: Int): LiveData<RecentContact> {
        return messagingDao.getRecentContact(recipientId)
    }

    override fun getMessagesSource(recipientId: Int): LiveData<PagedList<Message>> {
        return LivePagedListBuilder(messagingDao.getMessagesWithUser(recipientId), config)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<Message>() {
                override fun onItemAtEndLoaded(itemAtEnd: Message) {
                    loadPreviousMessages(recipientId, itemAtEnd.id)
                }
            })
            .build()
    }

    override fun loadPreviousMessages(recipientId: Int, lastMessageId: Int) {
        compositeDisposable += messagingApi.listMessages(
            recipientId,
            DEFAULT_PAGE_LIMIT,
            lastMessageId
        )
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.io())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        messagingDao.saveMessageList(result.data)
                    },
                    onFailure = {
                        Log.e(tag, "Can't load messages. $it")
                    }
                )
            }, { error ->
                Log.e(tag, "Can't load messages", error)
            })
    }

    override fun loadMessages(recipientId: Int, callback: ServiceCallback<Boolean>) {
        compositeDisposable += messagingApi.listMessages(recipientId, DEFAULT_PAGE_LIMIT)
            .subscribeOn(schedulers.io())
            .map { response ->
                val data = response.body()?.data
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        messagingDao.saveMessageList(result.data)
                    },
                    onFailure = {
                        throw Exception(Throwable(it))
                    }
                )
                data
            }
            .observeOn(schedulers.main())
            .subscribe({ data ->
                callback.onSuccess(data != null)
            }, { error ->
                Log.e(tag, "Can't load messages", error)
                callback.onFailure(error.getNetworkCallErrorMessage(application))
            })
    }

    override fun loadNewMessages(recipientId: Int) {
        compositeDisposable += messagingDao.getLastMessage(recipientId)
            .subscribeOn(schedulers.io())
            .flatMapSingle { lastMessage ->
                messagingApi.newMessages(recipientId, lastMessage.id)
            }
            .map { response ->
                val data = response.body()?.data
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        messagingDao.saveMessageList(result.data)
                    },
                    onFailure = {
                        throw Exception(Throwable(it))
                    }
                )
                data
            }
            .observeOn(schedulers.main())
            .subscribe({ }, { error ->
                Log.e(tag, "Can't load new messages", error)
            })
    }

    override fun sendMessage(
        createMessagePayload: CreateMessagePayload,
        callback: ServiceCallback<Message>
    ) {
        compositeDisposable += messagingApi.createMessage(CreateMessageDTO(createMessagePayload))
            .subscribeOn(schedulers.io())
            .map { response ->
                val data = response.body()?.data?.firstOrNull()
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        result.data.firstOrNull()?.let { messagingDao.saveMessage(it) }
                    },
                    onFailure = {
                        throw Exception(Throwable(it))
                    }
                )
                data
            }
            .observeOn(schedulers.main())
            .subscribe({ message ->
                if (message != null) callback.onSuccess(message)
            }, { error ->
                Log.e(tag, "Can't send message", error)
                callback.onFailure(error.getNetworkCallErrorMessage(application))
            })
    }

    override fun markAsRead(messageId: Int, callback: ServiceCallback<Message>) {
        compositeDisposable += messagingApi.markAsRead(MarkAsReadDTO(intArrayOf(messageId)))
            .subscribeOn(schedulers.io())
            .map { response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        result.data.firstOrNull()?.let { messagingDao.saveMessage(it) }
                    },
                    onFailure = {
                        throw Exception(Throwable(it))
                    }
                )
                val data = response.body()?.data?.firstOrNull()
                data
            }
            .observeOn(schedulers.main())
            .subscribe({ message ->
                if (message != null) callback.onSuccess(message)
            }, { error ->
                Log.e(tag, "Can't send message", error)
                callback.onFailure(error.getNetworkCallErrorMessage(application))
            })
    }

}