package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.ResetPasswordRequest

interface ResetPasswordApi {

    @GET("api/v1/user/request-otp")
    fun requestOTP(@Query("email") email:String): Single<Response<DataWrapper<Any>>>

    @PATCH("api/v1/user/reset-password")
    fun resetUserPassword(@Body resetPasswordRequest: ResetPasswordRequest): Single<Response<DataWrapper<Any>>>

}
