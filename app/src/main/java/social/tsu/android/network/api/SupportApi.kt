package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path
import social.tsu.android.network.model.SupportResponse

interface SupportApi {

    @POST("api/v2/posts/{id}/support")
    fun support(@Path("id") id: Long) : Single<Response<SupportResponse>>

}