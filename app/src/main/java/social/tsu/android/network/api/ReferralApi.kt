package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.InvitedContactsResponse
import social.tsu.android.network.model.ReferralResponse

interface ReferralApi {
    @GET("api/v1/referrals")
    fun getReferralLink(): Single<Response<DataWrapper<ReferralResponse>>>

    @GET("api/v1/invited_contacts")
    fun getInvitedContacts(): Single<Response<DataWrapper<InvitedContactsResponse>>>

    @POST("api/v1/invited_contacts")
    fun markContactInvited(@Query("hashed_contact_info") contact: String): Single<Response<DataWrapper<InvitedContactsResponse>>>
}
