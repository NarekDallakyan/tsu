package social.tsu.android.service

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.BuildConfig
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.data.local.models.TsuNotificationSubscriptions
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.NotificationsApi
import social.tsu.android.network.model.MarkReadRequest
import social.tsu.android.network.model.MarkSeenRequest
import social.tsu.android.network.model.NotificationFeedResponse
import social.tsu.android.network.model.UpdateSubscriptionRequest
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


abstract class TsuNotificationsService: DefaultService() {

    companion object {
        @JvmStatic
        val unseenNotificationsLiveData = MutableLiveData<Int>()
    }

    abstract fun getMyNotifications(
        cursor: String? = null,
        count: Int? = null,
        serviceCallback: ServiceCallback<NotificationFeedResponse>
    )

    abstract fun getMyNotificationsFromSummary(
        serviceCallback: ServiceCallback<List<TsuNotification>>
    )

    abstract fun getMyNotificationSubscriptions(serviceCallback: ServiceCallback<TsuNotificationSubscriptions>)

    abstract fun updateEmailSubscription(shouldReceive:Boolean, serviceCallback: ServiceCallback<Boolean>)

    abstract fun updateNotificationSubscriptions(topics:Map<String,Boolean>, serviceCallback: ServiceCallback<Boolean>)

    abstract fun markAsSeen(until:Long, serviceCallback: ServiceCallback<Boolean>)

    abstract fun markAsRead(notificationId:Int, serviceCallback: ServiceCallback<Boolean>)

}

class DefaultTsuNotificationsService @Inject constructor(
    private val application: Application, private val notificationsApi: NotificationsApi,
    private val schedulers: RxSchedulers
) : TsuNotificationsService() {

    override val tag: String = "DefaultTsuNotificationsService"

    override val compositeDisposable = CompositeDisposable()

    override fun getMyNotifications(
        cursor: String?,
        count: Int?,
        serviceCallback: ServiceCallback<NotificationFeedResponse>
    ) {
        AuthenticationHelper.currentUserId?.let {
            compositeDisposable += notificationsApi.getNotifications(it, cursor, count)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe(
                    { response ->
                        handleResponse(
                            application,
                            response,
                            onSuccess = { result ->
                                if (BuildConfig.DEBUG) Log.d(tag, "getMyNotifications $result")
                                serviceCallback.onSuccess(result)
                            }
                        )
                    },
                    { err ->
                        if (BuildConfig.DEBUG) Log.e(tag, "getMyNotifications $err")
                        serviceCallback.onFailure(err.localizedMessage?:err.message?:"Unknown error")
                    }
                )
        }

    }

    override fun getMyNotificationsFromSummary(
        serviceCallback: ServiceCallback<List<TsuNotification>>
    ) {
        AuthenticationHelper.currentUserId?.let {
            compositeDisposable += notificationsApi.getNotificationsSummary(it)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe(
                    { response ->
                        handleResponse(
                            application,
                            response,
                            onSuccess = { result ->
                                if (BuildConfig.DEBUG) Log.d(tag, "getMyNotificationsFromSummary $result")
                                val summary = result.summary
                                unseenNotificationsLiveData.postValue(result.unseenCount)
                                val notifications = listOf(
                                    *summary?.friendRequests?.toTypedArray() ?: arrayOf(),
                                    *summary?.general?.toTypedArray() ?: arrayOf(),
                                    *summary?.messages?.toTypedArray() ?: arrayOf()
                                )
                                serviceCallback.onSuccess(notifications)
                            }
                        )
                    },
                    { err ->
                        if (BuildConfig.DEBUG) Log.e(tag, "getMyNotificationsFromSummary $err")
                        handleApiCallError(application, err, serviceCallback)
                    }
                )
        }

    }

    override fun getMyNotificationSubscriptions(serviceCallback: ServiceCallback<TsuNotificationSubscriptions>) {
        AuthenticationHelper.currentUserId?.let {
            compositeDisposable += notificationsApi.getMySubscriptions(it)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe(
                    { response ->
                        handleResponse(application, response, serviceCallback)
                    },
                    { err ->
                        Log.d(tag, err.toString())
                        handleApiCallError(application, err, serviceCallback)
                    }
                )
        }
    }

    override fun updateEmailSubscription(
        shouldReceive: Boolean,
        serviceCallback: ServiceCallback<Boolean>
    ) {
        AuthenticationHelper.currentUserId?.let {
            compositeDisposable += notificationsApi.updateSubscriptions(
                it,
                UpdateSubscriptionRequest(email_digests = shouldReceive)
            )
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe(
                    { response ->
                        handleResponseResult(application, response, serviceCallback)
                    },
                    { err ->
                        Log.d(tag, err.toString())
                        handleApiCallError(application, err, serviceCallback)
                    }
                )
        }
    }

    override fun updateNotificationSubscriptions(
        topics: Map<String, Boolean>,
        serviceCallback: ServiceCallback<Boolean>
    ) {

        AuthenticationHelper.currentUserId?.let {
            compositeDisposable += notificationsApi.updateSubscriptions(
                it,
                UpdateSubscriptionRequest(topics)
            )
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe(
                    { response ->

                        handleResponseResult(application, response, serviceCallback)
                    },
                    { err ->
                        Log.d(tag, err.toString())
                        handleApiCallError(application, err, serviceCallback)
                    }
                )
        }
    }

    override fun markAsSeen(until: Long, serviceCallback: ServiceCallback<Boolean>) {

        AuthenticationHelper.currentUserId?.let {
            compositeDisposable += notificationsApi.markAsSeen(
                it,
                MarkSeenRequest(until)
            )
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe(
                    { response ->
                        unseenNotificationsLiveData.postValue(0)
                        handleResponseResult(application, response, serviceCallback)
                    },
                    { err ->
                        Log.d(tag, err.toString())
                        handleApiCallError(application, err, serviceCallback)
                    }
                )
        }
    }

    override fun markAsRead(notificationId: Int, serviceCallback: ServiceCallback<Boolean>) {

        AuthenticationHelper.currentUserId?.let {
            compositeDisposable += notificationsApi.markAsRead(
                it,
                MarkReadRequest(notificationId.toLong())
            )
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe(
                    { response ->
                        handleResponseResult(application, response, serviceCallback)
                    },
                    { err ->
                        Log.d(tag, err.toString())
                        handleApiCallError(application, err, serviceCallback)
                    }
                )
        }
    }
}