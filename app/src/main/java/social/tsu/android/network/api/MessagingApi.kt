package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.network.model.*

interface MessagingApi {

    @GET("api/v1/messages/contacted-users")
    fun fetchMyRecentContacts(): Single<Response<DataWrapper<List<RecentContact>>>>

    @POST("api/v1/messages/create")
    fun createMessage(
        @Body createMessageDTO: CreateMessageDTO
    ): Single<Response<DataWrapper<List<Message>>>>

    @GET("api/v1/messages/show/{recipient_id}")
    fun listMessages(
        @Path("recipient_id") recipientId: Int,
        @Query("limit") limit: Int
    ): Single<Response<DataWrapper<List<Message>>>>

    @GET("api/v1/messages/show/{recipient_id}")
    fun newMessages(
        @Path("recipient_id") recipientId: Int,
        @Query("after") lastMessageId: Int
    ): Single<Response<DataWrapper<List<Message>>>>

    @GET("api/v1/messages/show/{recipient_id}")
    fun listMessages(
        @Path("recipient_id") recipientId: Int,
        @Query("limit") limit: Int,
        @Query("before") beforeMessageId: Int
    ): Single<Response<DataWrapper<List<Message>>>>

    @POST("api/v1/messages/mark")
    fun markAsRead(@Body markAsReadDTO: MarkAsReadDTO): Single<Response<DataWrapper<List<Message>>>>

    @DELETE("api/v1/conversation/{recipient_id}")
    fun deleteConversation(
        @Path("recipient_id") recipientId: Int
    ): Single<Response<DataWrapper<TsuMessage>>>

}