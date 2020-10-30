package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import social.tsu.android.network.model.AppConfiguration

interface ConfigurationApi {

    @GET("api/v1/settings/app-configuration")
    fun getConfiguration(): Single<Response<AppConfiguration>>
}