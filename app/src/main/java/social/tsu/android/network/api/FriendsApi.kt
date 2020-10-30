package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.FollowResponse
import social.tsu.android.network.model.FriendRequestResponse
import social.tsu.android.network.model.TsuMessage

interface FriendsApi {
    @GET("api/v1/friends/{followUserId}/follow")
    fun followUserId(
        @Path("followUserId") followUserId: Int): Single<Response<FollowResponse>>

    @GET("api/v1/friends/{followUserId}/unfollow")
    fun unFollowUserId(
        @Path("followUserId") followUserId: Int): Single<Response<FollowResponse>>

    // type: request / cancel / accept / decline
    @GET("api/v1/friends/{type}/{friendUserId}")
    fun friendUserId(
        @Path("type") type: String,
        @Path("friendUserId") friendUserId: Int): Single<Response<DataWrapper<TsuMessage>>>

    @GET("api/v1/friends/pending")
    fun pendingFriendRequests(): Single<Response<DataWrapper<FriendRequestResponse>>>

    @DELETE("api/v1/friends/{friend_id}")
    fun unFriendUserId(
        @Path("friend_id") friendUserId: Int
    ): Single<Response<DataWrapper<TsuMessage>>>
}

enum class FriendRequestType {
    accept,
    decline,
    cancel,
    request
}
