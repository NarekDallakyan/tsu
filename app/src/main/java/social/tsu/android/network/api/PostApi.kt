package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import social.tsu.android.data.local.entity.Post
import social.tsu.android.data.local.entity.PostPayload
import social.tsu.android.data.local.entity.PostsPayload
import social.tsu.android.network.model.*

interface PostApi {

    companion object {
        const val MAX_HASHTAG_COUNT = 10
    }

    @GET("api/v2/posts/feed?format=json")
    fun getFeedV2Chrono(
        @Query("start") start: Int?,
        @Query("count") count: Int = 10
    ): Single<Response<DataWrapper<PostsResponseV2>>>

    @GET("api/v3/posts/feed?format=json")
    fun getFeedV3Trending(
        @Query("start") start: Int?,
        @Query("count") count: Int = 10,
        @Query("refresh_feed") refreshFeed: Boolean
    ): Single<Response<DataWrapper<PostsResponseV2>>>

    @GET("api/v1/posts/list/{user_id}")
    fun getUserFeed(
        @Path("user_id") id: Int,
        @Query("before") postId: Int?,
        @Query("count") count: Int = 10
    ): Single<Response<DataWrapper<PostsPayload>>>

    @GET("/api/v1/user/{user_id}/photos")
    fun getUserPhotos(
        @Path("user_id") userId: Int,
        @Query("page_key") pageKey: Int?,
        @Query("count") count: Int = 10
    ): Single<Response<UserMediaResponse>>

    @GET("/api/v1/user/{user_id}/video_streams")
    fun getUserVideos(
        @Path("user_id") userId: Int,
        @Query("page_key") pageKey: Int?,
        @Query("count") count: Int = 10
    ): Single<Response<UserMediaResponse>>

    @GET("api/v1/posts/post/{postId}")
    fun getPost(
        @Path("postId") postId: Int
    ): Single<Response<DataWrapper<Post>>>


    @GET("api/v1/posts/post/{postId}")
    fun getPostSync(
        @Path("postId") postId: Int
    ): Call<DataWrapper<Post>>

    @POST("api/v1/posts?format=json")
    fun createPost(
        @Body payload: CreatePostPayload
    ): Single<Response<DataWrapper<PostPayload>>>

    @PATCH("api/v2/posts/{postId}")
    fun editPostContent(
        @Path("postId") postId: Long,
        @Body editPostDTO: EditPostDTO
    ): Single<Response<DataWrapper<EditPostResponse>>>

    @DELETE("api/v1/posts/{postId}")
    fun deletePost(
        @Path("postId") postId: Long
    ): Single<Response<SuccessResultDTO>>

    @POST("api/v1/posts/report")
    fun reportPost(@Body payload: ReportPostPayload): Single<Response<DataWrapper<Any>>>

    @GET("api/v1/posts/like-list")
    fun getPostLikes(@Query("post_id") postId: Int): Single<Response<DataWrapper<List<UserProfile>>>>

    @GET("api/v2/posts/{postId}/support_list")
    fun getPostSupports(@Path("postId") postId: Int): Single<Response<DataWrapper<List<UserProfile>>>>

    @GET("api/v1/posts/hashtag")
    fun getPosts(
        @Query("hashtag") hashtag: String,
        @Query("page_key") page: Int
    ): Single<Response<HashtagResponse>>

    @GET("api/v1/posts/hashtag")
    fun getHashtagPosts(
        @Query("hashtag") hashtag: String,
        @Query("page_key") page: Int?
    ): Single<Response<UserMediaResponse>>

    @GET("api/v1/posts/discovery")
    fun getDiscoveryFeed(@Query("page_key[popular]")page_key : Int?,  @Query("count") count: Int = 50) : Single<Response<DiscoveryFeedResponse>>

}
