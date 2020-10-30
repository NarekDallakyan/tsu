package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import social.tsu.android.network.model.*

interface UserSettingsApi {

    @POST("api/v1/settings/info?format=json")
    fun updateUserInfo(
        @Body payload: ProfileEditInfoDTO
    ): Single<Response<DataWrapper<UserProfile>>>

    @POST("api/v1/settings/general")
    fun updateAccountInfo(
        @Body payload: AccountInfoDTO
    ): Single<Response<DataWrapper<UserProfile>>>

    @POST("api/v1/user/{user_id}/device_token")
    fun updateDeviceToken(
        @Path("user_id") userId: Int,
        @Body payload: TokenUpdateRequest
    ): Single<Response<DataWrapper<Boolean>>>

    @POST("api/v1/user/{user_id}/device_token")
    fun updateDeviceTokenSync(
        @Path("user_id") userId: Int,
        @Body payload: TokenUpdateRequest
    ): Call<DataWrapper<Boolean>>

    @DELETE("api/v1/user/{user_id}")
    fun deleteAccount(
        @Path("user_id") userId: Int,
        @Query("password") password: String
    ): Single<Response<DataWrapper<Boolean>>>


    @GET("api/v1/livestream/generate_chat_token")
    fun generateChatToken(@Query("username") username: String): Single<Response<DataWrapper<ChatTokenResponse>>>


}