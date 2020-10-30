package social.tsu.android.notifications

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.android.AndroidInjection
import social.tsu.android.BuildConfig
import social.tsu.android.data.local.entity.ResourceType
import social.tsu.android.data.local.entity.TsuNotificationResource
import social.tsu.android.data.local.models.TsuNotificationType
import social.tsu.android.data.repository.MessagingRepository
import social.tsu.android.data.repository.TsuNotificationRepository
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.api.UserApi
import social.tsu.android.network.api.UserSettingsApi
import social.tsu.android.network.model.TokenUpdateRequest
import social.tsu.android.service.ChatService
import javax.inject.Inject


private const val TAG = "FCMService"

class FCMService : FirebaseMessagingService() {

    companion object {

        private val _lastNotificationData = MutableLiveData<TsuRemoteMessageData>()
        val lastNotificationData: LiveData<TsuRemoteMessageData> = _lastNotificationData
    }

    @Inject
    lateinit var tsuNotificationRepo: TsuNotificationRepository

    @Inject
    lateinit var userSettingsApi: UserSettingsApi

    @Inject
    lateinit var userApi: UserApi

    @Inject
    lateinit var postApi: PostApi

    @Inject
    lateinit var messagingRepository: MessagingRepository
    @Inject
    lateinit var chatService: ChatService

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AuthenticationHelper.currentUserId?.let {
            val response =
                userSettingsApi.updateDeviceTokenSync(it, TokenUpdateRequest("android", token))
                    .execute()
            val msg =
                if (response.isSuccessful) "Update token successful" else "update token failed"
            Log.d(TAG, msg)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        setUpAndShowNotifications(remoteMessage)

    }

    private fun setUpAndShowNotifications(remoteMessage: RemoteMessage){
        synchronized(this){
            tsuNotificationRepo.refreshNotifications()

            val tsuRemoteMessage =
                Gson().fromJson(remoteMessage.data["object"], TsuRemoteMessageData::class.java)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "New notification with type: ${tsuRemoteMessage.type}")
            }
            _lastNotificationData.postValue(tsuRemoteMessage)

            if (tsuRemoteMessage.type == TsuNotificationType.NEW_DIRECT_MESSAGE) {
                messagingRepository.retry()

                tsuRemoteMessage.resource?.parameters?.userId?.let { senderId ->
                    chatService.loadNewMessages(senderId)
                }
            }

            val largeIcon = try {
                when (tsuRemoteMessage.resource?.resourceType) {
                    ResourceType.POST -> {
                        postApi.getPostSync(tsuRemoteMessage.resource?.id?.toIntOrNull() ?: 0)
                            .execute().body()?.data?.picture_url?.downloadImage(this)
                    }
                    ResourceType.USER -> {
                        userApi.getUserInfoSync(tsuRemoteMessage.resource?.id?.toIntOrNull() ?: 0)
                            .execute().body()?.data?.profilePictureUrl?.downloadImage(this)
                    }
                    else -> null
                }
            }catch (e:Exception) {null}


            val message = remoteMessage.data["message"]?:""

            NotificationsHelper.buildNotification(this, message, largeIcon, tsuRemoteMessage.type)
        }
    }

    private fun String.downloadImage(context:Context): Bitmap? {
        return try {
            Glide.with(context)
                .asBitmap()
                .load(this)
                .circleCrop()
                .submit()
                .get()
        }catch (e:Exception){
            e.printStackTrace()
            null
        }

    }

    class TsuRemoteMessageData{
        var resource :TsuNotificationResource?= null
        var topic: String? = null
        val type:TsuNotificationType
            get() = TsuNotificationType.values().firstOrNull { it.key == topic }?:TsuNotificationType.NEW_DIRECT_MESSAGE
    }
}