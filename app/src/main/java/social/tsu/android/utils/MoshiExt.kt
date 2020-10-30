package social.tsu.android.utils

import com.google.gson.Gson
import com.squareup.moshi.Moshi
import retrofit2.Response
import social.tsu.android.network.model.TsuError


inline fun <reified T> Moshi.fromJson(string: String?): T? {
    if (string != null) {
        return adapter(T::class.java).fromJson(string)
    }
    return null
}

fun Moshi.errorFromResponse(response: Response<*>): TsuError? {
    val errorJson = response.errorBody()?.string()
    return try {
        fromJson<TsuError>(errorJson)
    } catch (e: Exception) {
        TsuError(true, response.message())
    }
}

fun Response<*>.errorFromResponse(): TsuError?{
    val gson = Gson()
    return try{
        gson.fromJson(body()?.let { gson.toJson(it) }?:errorBody()?.string(), TsuError::class.java)
    } catch (e:Exception){ TsuError(true, message())}
}