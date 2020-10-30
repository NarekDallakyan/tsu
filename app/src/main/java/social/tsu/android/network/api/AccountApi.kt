package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import social.tsu.android.network.model.Account
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.PayPalValidationResponse
import social.tsu.android.network.model.TsuMessage

interface AccountApi {

    @GET("api/v1/account/bank")
    fun getAccountBalance(): Single<Response<DataWrapper<Account>>>

    @POST("api/v1/account/paypalvalidation")
    fun validatePayPal(@Body payload: Any?): Single<Response<DataWrapper<PayPalValidationResponse>>>

    @POST("api/v1/account/redeem")
    fun requestAccountRedemption(@Query("amount") amount: Double = 0.00): Single<Response<TsuMessage>>
}
