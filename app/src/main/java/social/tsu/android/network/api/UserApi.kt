package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.ReportUserPayload
import social.tsu.android.network.model.UserMediaResponse
import social.tsu.android.network.model.UserProfile

interface UserApi {

    @GET("api/v1/user/info")
    fun getUserInfo(
        @Query("user_id") userId: Int
    ): Single<Response<DataWrapper<UserProfile>>>

    @GET("api/v1/user/info")
    fun getTagUserInfo(
        @Query("username") userTagString: String
    ): Single<Response<DataWrapper<UserProfile>>>


    @GET("/api/v1/user/{user_id}/photos")
    fun getUserPhotos(
        @Path("user_id") userId: Int,
        @Query("page_key") pageKey: Int?,
        @Query("count") count: Int = 10
    ): Single<Response<UserMediaResponse>>

    @GET("api/v1/user/info")
    fun getUserInfoSync(
        @Query("user_id") userId: Int
    ): Call<DataWrapper<UserProfile>>

    @GET("/api/v1/user/{user_id}/friends")
    fun getUserFriends(
        @Path("user_id") userId: Int,
        @Query("page") page: Int,
        @Query("count") count: Int
    ): Single<Response<DataWrapper<List<UserProfile>>>>

    @GET("/api/v1/user/{user_id}/followers")
    fun getUserFollowers(
        @Path("user_id") userId: Int,
        @Query("page") page: Int,
        @Query("count") count: Int
    ): Single<Response<DataWrapper<List<UserProfile>>>>

    @GET("/api/v1/user/{user_id}/followings")
    fun getUserFollowings(
        @Path("user_id") userId: Int,
        @Query("page") page: Int,
        @Query("count") count: Int
    ): Single<Response<DataWrapper<List<UserProfile>>>>

    @POST("/api/v1/user/report")
    fun reportUser(@Body reportUserPayload: ReportUserPayload): Single<Response<DataWrapper<DataWrapper<Any>>>>
}
