package social.tsu.android.network.api

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url
import social.tsu.android.network.model.DacastResponse
import social.tsu.android.network.model.DataWrapper

interface LiveApi {

    @GET("https://json.dacast.com/b/148510/p/268967")
    fun getDacast() : Single<ResponseBody>
    @GET("https://services.dacast.com/token/i/b/148510/p/268967")
    fun getDacastToken() : Single<DacastResponse>
    @GET("api/v1/playlist")
    fun getPlaylist(): Single<Response<DataWrapper<HlsOffsetResponse>>>
    @GET
    fun getPlaybackHls(@Url url: String): Single<Response<HlsResponse>>
}

data class HlsOffsetResponse(val hls: String, val offset: String)
data class HlsResponse(val hls: String)