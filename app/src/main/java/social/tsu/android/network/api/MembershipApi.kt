package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.MembershipResponse
interface MembershipApi {
    @GET("api/v1/memberships")
    fun getMyCommunities() : Single<Response<MembershipResponse>>

    @PUT("api/v1/memberships/{membership_id}/accept_promotion")
    fun acceptPromotion(@Path("membership_id") membershipId: Int): Single<Response<DataWrapper<Any>?>>

    @PUT("api/v1/memberships/{membership_id}/decline_promotion")
    fun declinePromotion(@Path("membership_id") membershipId: Int): Single<Response<DataWrapper<Any>?>>

    @PUT("api/v1/memberships/{membership_id}/accept")
    fun acceptMembership(@Path("membership_id") membershipId: Int): Single<Response<DataWrapper<Any>?>>

    @PUT("api/v1/memberships/{membership_id}/refuse")
    fun declineMembership(@Path("membership_id") membershipId: Int): Single<Response<DataWrapper<Any>?>>

    @DELETE("api/v1/memberships/{membership_id}")
    fun deleteMembership(@Path("membership_id") membershipId: Int): Single<Response<DataWrapper<Any>?>>

    @PUT("api/v1/memberships/{membership_id}/demote")
    fun demoteMembership(@Path("membership_id") membershipId: Int): Single<Response<DataWrapper<Any>?>>

    @PUT("api/v1/memberships/{membership_id}/promote")
    fun promoteMembership(@Path("membership_id") membershipId: Int): Single<Response<DataWrapper<Any>?>>
}