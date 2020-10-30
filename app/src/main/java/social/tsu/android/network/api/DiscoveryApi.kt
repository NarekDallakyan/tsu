package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import social.tsu.android.data.local.entity.PostsPayload
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.DiscoveryFeedResponse
import social.tsu.android.network.model.HashtagResponse

interface DiscoveryApi {

    @GET("api/v1/posts/discovery")
    fun getDiscoveryFeed(@Query("page_key[popular]")page_key : Int?) : Single<Response<DiscoveryFeedResponse>>
}