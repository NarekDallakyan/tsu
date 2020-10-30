package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.POST
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.StreamResponse

interface StreamApi {

    @POST("api/v1/streams")
    fun createStream(): Single<Response<DataWrapper<StreamResponse>>>
}
