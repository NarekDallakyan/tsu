package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import social.tsu.android.network.model.CreateAccountResponse
import social.tsu.android.network.model.LoginRequest

interface AuthenticationApi {
    @POST("api/v1/user/login")
    fun login(@Body payload: LoginRequest): Single<Response<CreateAccountResponse>>

    @POST("api/v1/user/login")
    fun refreshToken(@Body payload: LoginRequest): Call<CreateAccountResponse>
}
