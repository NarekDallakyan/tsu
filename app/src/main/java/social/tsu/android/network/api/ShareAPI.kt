package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.PUT
import retrofit2.http.Path
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.ShareResponseDTO

interface ShareAPI {

    @PUT("/api/v1/posts/{postId}/share")
    fun shareUserPost(
        @Path("postId") postId: Long
    ): Single<Response<DataWrapper<ShareResponseDTO>>>

    @PUT("/api/v1/posts/{postSharedId}/unshare")
    fun unshareUserPost(
        @Path("postSharedId") postSharedId: Int
    ): Single<Response<DataWrapper<ShareResponseDTO>>>

}