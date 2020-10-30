package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST
import social.tsu.android.network.model.BlockUserPayload
import social.tsu.android.network.model.BlockUserResponse
import social.tsu.android.network.model.UnblockUserPayload

interface BlockApi {

    @POST("api/v1/blocks")
    fun blockUser(@Body payload: BlockUserPayload) : Single<Response<BlockUserResponse>>

    //Just @DELETE wont work. Receiving 'Non-body HTTP method cannot contain @Body.' error
    @HTTP(method = "DELETE", path = "api/v1/blocks", hasBody = true)
    fun unblockUser(@Body payload: UnblockUserPayload) : Single<Response<BlockUserResponse>>
}