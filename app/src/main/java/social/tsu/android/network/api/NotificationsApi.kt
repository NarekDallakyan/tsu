package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*
import social.tsu.android.data.local.models.TsuNotificationSubscriptions
import social.tsu.android.network.model.*

interface NotificationsApi {

    @GET("api/v2/users/{id}/notifications/subscriptions")
    fun getMySubscriptions(@Path("id") userId:Int): Single<Response<TsuNotificationSubscriptions>>

    @PUT("api/v2/users/{id}/notifications/subscriptions")
    fun updateSubscriptions(@Path("id") userId:Int, @Body updateSubscriptionRequest: UpdateSubscriptionRequest): Single<Response<DataWrapper<Boolean>>>

    @GET("api/v2/users/{id}/notifications/feed")
    fun getNotifications(
        @Path("id") userId: Int,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Single<Response<NotificationFeedResponse>>

    @GET("api/v2/users/{id}/notifications/summary")
    fun getNotificationsSummary(@Path("id") userId:Int): Single<Response<NotificationSummaryResponse>>

    @PATCH("/api/v2/users/{id}/notifications/seen")
    fun markAsSeen(@Path("id") userId:Int, @Body markSeenRequest: MarkSeenRequest): Single<Response<DataWrapper<Boolean>>>

    @PATCH("/api/v2/users/{id}/notifications/read")
    fun markAsRead(@Path("id") userId:Int, @Body markReadRequest: MarkReadRequest): Single<Response<DataWrapper<Boolean>>>

}