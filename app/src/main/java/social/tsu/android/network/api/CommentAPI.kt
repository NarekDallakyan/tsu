package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*
import social.tsu.android.network.model.Comment
import social.tsu.android.network.model.CommentDTO
import social.tsu.android.network.model.DataWrapper

interface CommentAPI {
    @POST("api/v1/comments/{postId}")
    fun createComment(
        @Path("postId") postId: Long,
        @Body payload: CommentDTO
    ): Single<Response<DataWrapper<Comment>>>

    @GET("api/v1/comments/{postId}")
    fun getPostComments(
        @Path("postId") postId: Long,
        @Query("since") postTimestamp: Int
    ): Single<Response<DataWrapper<List<Comment>>>>

    @GET("api/v1/comments/{postId}")
    fun getMorePostComments(
        @Path("postId") postId: Long,
        @Query("after") lastCommentId: Int,
        @Query("count") count: Int = 25
    ): Single<Response<DataWrapper<List<Comment>>>>

    @PATCH("api/v1/comments/{commentId}/like")
    fun likeComment(
        @Path("commentId") commentId: Int
    ): Single<Response<DataWrapper<Boolean>>>

    @PATCH("api/v1/comments/{commentId}/unlike")
    fun unlikeComment(
        @Path("commentId") commentId: Int
    ): Single<Response<DataWrapper<Boolean>>>

    @DELETE("api/v1/comments/{commentId}")
    fun deleteComment(
        @Path("commentId") commentId: Int
    ): Single<Response<DataWrapper<Boolean>>>

}