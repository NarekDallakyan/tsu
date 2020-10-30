package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.PUT
import retrofit2.http.Path
import social.tsu.android.network.model.LikeResponse

interface LikeApi {

    @PUT("api/v1/posts/{postId}/like")
    fun likeUserPost(
        @Path("postId") postId: Long
    ): Single<Response<LikeResponse>>

    @PUT("api/v1/posts/{postId}/unlike")
    fun unlikeUserPost(
        @Path("postId") postId: Long
    ): Single<Response<LikeResponse>>

}