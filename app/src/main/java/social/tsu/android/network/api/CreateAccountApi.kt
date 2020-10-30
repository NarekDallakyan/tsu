package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import social.tsu.android.data.local.models.OldUserDetails
import social.tsu.android.network.model.*

interface CreateAccountApi {

    @POST("/api/v1/user/signup")
    fun createAccount(@Body payload: CreateAccountRequest): Single<Response<CreateAccountResponse>>

    @GET("/api/v1/user/check-email")
    fun checkIfEmailIsUnique(@Query("email") email: String): Single<Response<EmailUniqueResponse>>

    @GET("/api/v1/user/check-email")
    fun checkIfEmailIsUniqueWrapper(@Query("email") email: String): Single<Response<DataWrapper<EmailUnique>>>

    @GET("/api/v1/user/check-username")
    fun checkIfUsernameUnique(@Query("username") email: String): Single<Response<UsernameUniqueResponse>>

    @POST("api/v1/user/new_flow")
    fun verifyInviteRequest(@Body verifyInviteRequest: VerifyInviteRequest): Single<Response<VerifyInviteResponse>>

    @POST("api/v1/old_tsu_users")
    fun verifyOldUserRequest(@Body verifyOldTsuUserRequest: VerifyOldTsuUserRequest): Single<Response<DataWrapper<OldUserDetails>>>

}

