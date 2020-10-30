package social.tsu.android.network

sealed class ResponseWrapper<out T> {
    data class Success<out T>(val response: T) : ResponseWrapper<T>()
    data class Error(val code: Int? = null, val error: ErrorResponse? = null) : ResponseWrapper<Nothing>()
    object NetworkError : ResponseWrapper<Nothing>()
    object Empty : ResponseWrapper<Nothing>()
}

data class ErrorResponse(val error: Boolean, val message: String?)
//
//suspend fun <T> callWithErrorHandling(
//    dispatcher: CoroutineDispatcher,
//    apiCall: suspend () -> Response<T>
//): ResponseWrapper<T> {
//
//    return withContext(dispatcher) {
//
//        try {
//            val response = apiCall.invoke()
//            if (response.isSuccessful) {
//                response.body()?.let {
//                    ResponseWrapper.Success(it)
//
//                } ?:  ResponseWrapper.Empty
//
//            } else {
//                val errorResponse = response.errorBody()?.let {
//                    parseBufferedSource(it.source())
//                }
//                ResponseWrapper.Error(response.code(), errorResponse)
//
//            }
//
//        } catch (t: Throwable) {
//
//            when (t) {
//                is IOException -> ResponseWrapper.NetworkError
//                is HttpException -> {
//                    val code = t.code()
//                    val errorResponse = parseThrowable(t)
//                    ResponseWrapper.Error(code, errorResponse)
//                }
//                else -> {
//                    ResponseWrapper.Error(null, null)
//                }
//            }
//        }
//    }
//}
//
//
//private fun parseThrowable(throwable: HttpException): ErrorResponse? {
//    return try {
//        throwable.response()?.errorBody()?.source()?.let {
//            parseBufferedSource(it)
//        }
//    } catch (exception: Exception) {
//        null
//    }
//}
//
//private fun parseBufferedSource(it: BufferedSource): ErrorResponse? {
//    val moshiAdapter = Moshi.Builder().build().adapter(ErrorResponse::class.java)
//    return moshiAdapter.fromJson(it)
//}
//
