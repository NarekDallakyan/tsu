package social.tsu.android.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.threeten.bp.Instant
import social.tsu.android.data.local.dao.TsuNotificationDao
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.data.local.entity.TsuSubscriptionTopic
import social.tsu.android.data.local.models.TsuNotificationSubscriptions
import social.tsu.android.network.model.NotificationFeedResponse
import social.tsu.android.service.FriendService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.TsuNotificationsService
import social.tsu.android.ui.model.Data
import java.util.concurrent.Executors
import javax.inject.Inject

class TsuNotificationRepository @Inject constructor(private val notificationsService: TsuNotificationsService,
                                                    private val friendService: FriendService,
                                                    private val notificationDao: TsuNotificationDao){

    private val _loadState = MutableLiveData<Data<Boolean>>()
    val loadState: LiveData<Data<Boolean>> = _loadState

    val unseenNotificationsCount by lazy {
        TsuNotificationsService.unseenNotificationsLiveData
//        notificationDao.getUnseenNotificationsCount()
    }

    private val notificationsBoundaryCallback = NotificationsBoundaryCallback(notificationsService, notificationDao,_loadState)

    val notifications by lazy {
        LivePagedListBuilder(notificationDao.getTsuNotifications(),config)
            .setBoundaryCallback(notificationsBoundaryCallback).build()
    }

    val notificationSubscriptions by lazy {
        refreshSubscriptions()
        LivePagedListBuilder(notificationDao.getSubscriptions(),config).build()
    }

    fun refreshNotifications() {
        _loadState.postValue(Data.Loading())

        notificationsService.getMyNotificationsFromSummary(object :
            ServiceCallback<List<TsuNotification>> {
            override fun onSuccess(result: List<TsuNotification>) {
                _loadState.postValue(Data.Success(true))
                execute {
                    notificationDao.saveTsuNotifications(*result.toTypedArray())
                }
            }

            override fun onFailure(errMsg: String) {
                _loadState.postValue(Data.Error(Throwable(errMsg)))
            }
        })
    }

    private fun refreshSubscriptions() {
        _loadState.postValue(Data.Loading())

        notificationsService.getMyNotificationSubscriptions( object :
            ServiceCallback<TsuNotificationSubscriptions> {
            override fun onSuccess(result: TsuNotificationSubscriptions) {
                _loadState.postValue(Data.Success(true))
                execute {
                    notificationDao.saveTsuSubscriptionTopics(result)
                }
            }

            override fun onFailure(errMsg: String) {
                _loadState.postValue(Data.Error(Throwable(errMsg)))
            }
        })
    }

    fun markAsSeen()= execute{
        val mostRecentNotification = notificationDao.getMostRecentNotification()

        if (mostRecentNotification?.seenAt != null) return@execute

        val seenAt = (mostRecentNotification?.timestamp?:Instant.now().epochSecond)+1
        notificationsService.markAsSeen(seenAt, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                execute { notificationDao.getUnseenNotifications().forEach {
                    notificationDao.markAsSeen(it.dbId)
                }}
            }

            override fun onFailure(errMsg: String) {
                Log.w("NotificationRepository", errMsg)
            }
        })
    }

    fun markAsRead(tsuNotification:TsuNotification){
        if(tsuNotification.readAt != null) return
        notificationsService.markAsRead(tsuNotification.id, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                execute { notificationDao.markAsRead(tsuNotification.dbId) }
            }

            override fun onFailure(errMsg: String) {
                Log.w("NotificationRepository", errMsg)
            }
        })
    }

    fun acceptFriendRequest(tsuNotification: TsuNotification): LiveData<Data<Boolean>>{
        val acceptLoadState = MutableLiveData<Data<Boolean>>()
        acceptLoadState.value = Data.Loading()
        val userId = tsuNotification.resource?.id?.toIntOrNull()?:-1


        if (userId == -1){
            acceptLoadState.value = Data.Success(true)
            execute { notificationDao.deleteNotification(tsuNotification) }
            return acceptLoadState
        }

        friendService.acceptFriend(userId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                acceptLoadState.value = Data.Success(true)
                execute { notificationDao.deleteNotification(tsuNotification) }
            }

            override fun onFailure(errMsg: String) {
                acceptLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return acceptLoadState
    }

    fun declineFriendRequest(tsuNotification: TsuNotification): LiveData<Data<Boolean>>{
        val declineLoadState = MutableLiveData<Data<Boolean>>()
        declineLoadState.value = Data.Loading()
        val userId = tsuNotification.resource?.id?.toIntOrNull()?:-1


        if (userId == -1){
            declineLoadState.value = Data.Success(true)
            execute { notificationDao.deleteNotification(tsuNotification) }
            return declineLoadState
        }

        friendService.declineFriend(userId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                declineLoadState.value = Data.Success(true)
                execute { notificationDao.deleteNotification(tsuNotification) }
            }

            override fun onFailure(errMsg: String) {
                declineLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return declineLoadState
    }

    private fun execute (block:()->Unit) = Executors.newSingleThreadExecutor().execute(block)

    fun retryNotificationFetch() {
        notificationsBoundaryCallback.retry()
    }

    fun updateSubscriptionStatus(tsuNotificationTopic: TsuSubscriptionTopic, shouldSubscribe:Boolean){
        if (tsuNotificationTopic.name == "all_notifications") return updateAllSubscriptions(shouldSubscribe)
        notificationsService.updateNotificationSubscriptions(mapOf(tsuNotificationTopic.name to shouldSubscribe),
            object : ServiceCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    _loadState.postValue(Data.Success(true))
                    execute {
                        val updatedTopic =
                            tsuNotificationTopic.apply { subscribed = shouldSubscribe }
                        notificationDao.saveTsuSubscriptionTopics(updatedTopic)
                    }
                }

                override fun onFailure(errMsg: String) {
                    _loadState.postValue(Data.Error(Throwable(errMsg)))
                }
            })
    }

    private fun updateAllSubscriptions(shouldSubscribe: Boolean) = execute {
        var notificationSubscriptions = notificationDao.getAllSubscriptions()
        notificationSubscriptions = notificationSubscriptions.subList(1,notificationSubscriptions.size)// exclude all notification
        val updateMap = mapOf(*notificationSubscriptions.map { it.name to shouldSubscribe }.toTypedArray())

        notificationsService.updateNotificationSubscriptions(updateMap,
            object : ServiceCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    _loadState.postValue(Data.Success(true))
                    refreshSubscriptions()
                }

                override fun onFailure(errMsg: String) {
                    _loadState.postValue(Data.Error(Throwable(errMsg)))
                }
            })

    }

    private var config: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(PAGE_SIZE)
        .setInitialLoadSizeHint(PAGE_SIZE)
        .setEnablePlaceholders(false)
        .build()

    companion object{
        const val PAGE_SIZE = 20
    }
}

class NotificationsBoundaryCallback(
    private val notificationsService: TsuNotificationsService,
    private val notificationDao: TsuNotificationDao,
    private val loadState:MutableLiveData<Data<Boolean>>
) : PagedList.BoundaryCallback<TsuNotification>() {

    companion object {
        private val TAG = NotificationsBoundaryCallback::class.java.simpleName
    }

    private var lastResponseCursor: String? = ""

    private var isRequestInProgress = false

    override fun onZeroItemsLoaded() {
        execute {
            requestAndSaveData()
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: TsuNotification) {
        execute {
            requestAndSaveData()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: TsuNotification) {}

    private fun requestAndSaveData() {

        if (isRequestInProgress || lastResponseCursor == null) return

        loadState.postValue(Data.Loading())
        isRequestInProgress = true
        notificationsService.getMyNotifications(
            lastResponseCursor,
            TsuNotificationRepository.PAGE_SIZE,
            object :
                ServiceCallback<NotificationFeedResponse> {
                override fun onSuccess(result: NotificationFeedResponse) {
                    loadState.postValue(Data.Success(true))

                    isRequestInProgress = false
                    lastResponseCursor = result.cursor
                    result.notifications?.let { list ->
                        execute { notificationDao.saveTsuNotifications(*list.toTypedArray()) }
                    }
                }

                override fun onFailure(errMsg: String) {
                    isRequestInProgress = false
                    loadState.postValue(Data.Error(Throwable(errMsg)))
                }
            })

    }

    fun retry(){
        requestAndSaveData()
    }

    private fun execute (block:()->Unit) = Executors.newSingleThreadExecutor().execute(block)

}