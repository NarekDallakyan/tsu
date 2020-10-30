package social.tsu.android.network.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import social.tsu.android.network.model.AnalyticsResponse
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.network.model.FamilyTreeResponse

interface AnalyticsApi {

    @GET("api/v1/account/tree")
    fun getFamilyTree(
        @Query("page") page: Int,
        @Query("count") count: Int
    ): Single<Response<DataWrapper<FamilyTreeResponse>>>

    @GET("api/v1/account/analytics")
    fun getAnalytics(
        @Query("from_date") date: String
    ): Single<Response<DataWrapper<AnalyticsResponse>>>

}