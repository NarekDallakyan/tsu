package social.tsu.android.network.api

import retrofit2.http.GET

interface FeedApi {
    @GET("api/v1/posts/post")
    fun getPost()
}