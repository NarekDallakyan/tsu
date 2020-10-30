package social.tsu.android.network.api

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import social.tsu.android.data.local.entity.PostPayload
import social.tsu.android.network.model.*

interface CommunityApi {

    @POST("api/v1/groups")
    fun createCommunity(@Body payload: CommunityPayload): Single<Response<DataWrapper<CommunityResponse>>>

    @GET("api/v1/groups/{id}")
    fun getCommunityInfo(@Path("id") id: Int): Single<Response<CommunityResponse>>

    @PATCH("api/v1/groups/{id}")
    fun updateCommunity(
        @Path("id") id: Int,
        @Body payload: CommunityPayload
    ): Single<Response<CommunityResponse>>

    @DELETE("api/v1/groups/{id}")
    fun deleteCommunity(@Path("id") id: Int): Single<Response<ResponseBody>>

    @GET("api/v1/groups/suggested")
    fun getSuggestedCommunities(): Single<Response<CommunityListResponse>>

    @GET("api/v1/groups/{user_id}/posts")
    fun getCommunityFeed(
        @Path("user_id") id: Int,
        @Query("start") nextPage: Int?
    ): Single<Response<CommunityFeedResponse>>

    @POST("api/v1/groups/{id}/memberships")
    fun joinCommunity(@Path("id") id: Int) : Single<Response<DataWrapper<Any>>>

    @PUT("/api/v1/memberships/{membership_id}/accept")
    fun acceptMembership(@Path("membership_id") id: Int) : Single<Response<DataWrapper<Any>>>

    @GET("api/v1/groups/{id}/pending_requests")
    fun getPendingRequests(@Path("id") id: Int) : Single<Response<CommunityPendings>>

    @POST("api/v1/groups/{id}/posts")
    fun createPost(@Path("id") id: Int, @Body payload: CreateCommunityPostPayload) : Single<Response<PostPayload>>

    @POST("api/v1/groups/{id}/posts")
    fun createImagePostJson(@Path("id") id: Int, @Body imagePost: CreateCommunityImagePostPayload): Single<Response<PostPayload>>

    @POST("api/v1/groups/{id}/memberships")
    fun inviteMember(
        @Path("id") id: Int,
        @Body payload: CommunityInviteMemberPayload
    ): Single<Response<DataWrapper<Any>>>

    @GET("api/v1/groups/{id}/pending_group_posts")
    fun getPendingPosts(@Path("id") communityId: Int): Single<Response<PendingPostsResponse>>

    @PUT("api/v1/groups/{communityId}/pending_group_posts/{id}/accept")
    fun approvePost(
        @Path("communityId") communityId: Int,
        @Path("id") postId: Int
    ): Single<Response<DataWrapper<Any>?>>

    @DELETE("api/v1/groups/{communityId}/pending_group_posts/{id}")
    fun declinePost(
        @Path("communityId") communityId: Int,
        @Path("id") postId: Int
    ): Single<Response<DataWrapper<Any>?>>

    @DELETE("api/v1/memberships/{id}")
    fun leave(@Path("id") membershipId: Int): Single<Response<DataWrapper<Any>>>

    @GET("api/v1/groups/{id}/members")
    fun getCommunityMembers(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("count") count: Int
    ): Single<Response<CommunityMembersResponse>>

    @POST("api/v1/groups/{id}/kick")
    fun kickMember(
        @Path("id") id: Int,
        @Body request: MemberKickRequest
    ): Single<Response<DataWrapper<Any>?>>
}