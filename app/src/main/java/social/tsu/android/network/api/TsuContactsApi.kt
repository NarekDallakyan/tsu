package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.network.model.DataWrapper

interface TsuContactsApi {

    @GET("api/v1/user/{user_id}/friends")
    fun fetchMyFriends(
        @Path("user_id") userId:Int,
        @Query("page") page: Int?,
        @Query("count") count: Int?
    ): Single<Response<DataWrapper<List<TsuContact>>>>

    @GET("api/v1/user/{user_id}/followers")
    fun fetchMyFollowers(
        @Path("user_id") userId:Int,
        @Query("page") page: Int?,
        @Query("count") count: Int?
    ): Single<Response<DataWrapper<List<TsuContact>>>>

    @GET("api/v1/user/{user_id}/followings")
    fun fetchMyFollowings(
        @Path("user_id") userId:Int,
        @Query("page") page: Int?,
        @Query("count") count: Int?
    ): Single<Response<DataWrapper<List<TsuContact>>>>

}