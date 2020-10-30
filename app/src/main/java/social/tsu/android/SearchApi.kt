package social.tsu.android

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import social.tsu.android.network.model.*


const val DEFAULT_SEARCH_LIMIT: Int = 100

interface SearchApi {

    @GET("/api/v1/search/users/{search_term}")
    fun searchUsers(
        @Path("search_term") userName: String,
        @Query("limit") limit: Int = DEFAULT_SEARCH_LIMIT
    ): Single<Response<DataWrapper<SearchUsersResponse>>>

    @GET("/api/v1/search/mentions/{search_term}")
    fun searchMentionUsers(
        @Path("search_term") userName: String,
        @Query("limit") limit: Int = DEFAULT_SEARCH_LIMIT
    ): Single<Response<DataWrapper<List<MentionUser>>>>

    @GET("/api/v1/search/hashtags/{search_term}")
    fun searchHashtags(
        @Path("search_term") hashtag: String,
        @Query("limit") limit: Int = DEFAULT_SEARCH_LIMIT
    ): Single<Response<DataWrapper<List<HashTag>>>>

    @GET("/api/v1/search/groups/{search_term}")
    fun searchGroups(
        @Path("search_term") groupName: String,
        @Query("topics_only") topicsOnly: Boolean = false,
        @Query("limit") limit: Int = DEFAULT_SEARCH_LIMIT
    ): Single<Response<DataWrapper<List<Group>>>>

    @GET("/api/v1/search/{search_term}")
    fun searchAny(
        @Path("search_term") query: String,
        @Query("limit") limit: Int = DEFAULT_SEARCH_LIMIT
    ): Single<Response<DataWrapper<SearchResponse>>>

    @GET("/api/v1/search/mentions/{term}")
    fun searchTag(
        @Path("term") query: String,
        @Query("limit") limit: Int = DEFAULT_SEARCH_LIMIT
    ): Single<Response<DataWrapper<SearchResponse>>>



}
