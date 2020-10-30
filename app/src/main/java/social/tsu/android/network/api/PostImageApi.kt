package social.tsu.android.network.api

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import social.tsu.android.network.model.ImagePostPayload
import social.tsu.android.data.local.entity.PostPayload
import social.tsu.android.network.model.*

interface PostImageApi {

    @Multipart
    @POST("api/v1/posts?format=json")
    fun createImagePost(
        @Part("content") message: RequestBody,
        @Part image: MultipartBody.Part
    ): Single<Response<DataWrapper<PostPayload>>>

    @POST("api/v1/posts?format=json")
    fun createImagePostJson(
        @Body imagePost: ImagePostPayload
    ): Single<Response<DataWrapper<PostPayload>>>

}
