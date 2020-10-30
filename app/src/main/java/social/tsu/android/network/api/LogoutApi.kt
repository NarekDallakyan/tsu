package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import social.tsu.android.network.model.DataWrapper

interface LogoutApi {

    @GET("/api/v1/user/signout")
    fun logoutUser(): Single<Response<DataWrapper<Any>>>
}
