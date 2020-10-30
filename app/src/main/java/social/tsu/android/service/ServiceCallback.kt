package social.tsu.android.service

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.MissingBackpressureException
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.exceptions.UndeliverableException
import retrofit2.Response
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.NetworkError
import social.tsu.android.network.model.DataWrapper
import social.tsu.android.utils.errorFromResponse
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface ServiceCallback <T>{

    fun onSuccess(result:T)

    fun onFailure(errMsg:String)

}

fun <T> handleResponseWithWrapper(
    context: Context,
    response: Response<DataWrapper<T>>,
    serviceCallback: ServiceCallback<T>
) {
    if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE) {
        Log.d("NetworkResponseHandler", response.body().toString())
        if (response.body()?.error == false) {
            if (response.body() == null) {
                serviceCallback.onFailure(context.getString(R.string.generic_error_message))
            } else {
                serviceCallback.onSuccess(response.body()!!.data)
            }
        } else {
            serviceCallback.onFailure(response.body()?.message ?: response.message())
        }
    } else {
        handleApiCallError(context, response, serviceCallback)
    }
}


fun <T> handleResponse(
    context: Context,
    response: Response<T>,
    serviceCallback: ServiceCallback<T>
) {
    if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE) {
        Log.d("NetworkResponseHandler", response.body().toString())
        response.body()?.let { serviceCallback.onSuccess(it) }
    } else {
        handleApiCallError(context, response, serviceCallback)
    }
}


fun <T> handleAnyResponseType(
    context: Context,
    response: Response<T>,
    serviceCallback: ServiceCallback<Any>
) {
    if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE) {
        Log.d("NetworkResponseHandler", response.body().toString())
        response.body()?.let { serviceCallback.onSuccess(it as Any) }
    } else {
        handleApiCallError(context, response, serviceCallback)
    }
}

fun handleResponseResult(
    context: Context,
    response: Response<*>,
    serviceCallback: ServiceCallback<Boolean>
) {
    Log.d("NetworkResponseHandler", response.body().toString())

    if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE && response.errorFromResponse()?.error != true) {
        Log.d("NetworkResponseHandler", response.body().toString())
        serviceCallback.onSuccess(true)
    } else {
        handleApiCallError(context, response, serviceCallback)
    }
}

fun <T> handleApiCallError(
    context: Context,
    error: Throwable,
    serviceCallback: ServiceCallback<T>
) {
    serviceCallback.onFailure(error.getNetworkCallErrorMessage(context))
}

fun <T> handleApiCallError(
    context: Context,
    response: Response<*>,
    serviceCallback: ServiceCallback<T>
) {
    serviceCallback.onFailure(response.getNetworkCallErrorMessage(context))
}

fun Response<*>.getNetworkCallErrorMessage(context: Context): String {
    val tsuError = errorFromResponse()
    return if (tsuError?.message == null || tsuError.message == message()) {
        getNetworkErrorMessage(context).errorMsg
    } else {
        tsuError.message
    }
}

fun Throwable.getNetworkCallErrorMessage(context: Context): String {
    return when (this) {
        is CompositeException, is UndeliverableException -> NetworkError.GENERIC
        is MissingBackpressureException -> NetworkError.GENERIC
        is OnErrorNotImplementedException -> NetworkError.GENERIC
        is SocketTimeoutException, is UnknownHostException -> NetworkError.TIMEOUT
        else -> NetworkError.GENERIC
    }.apply { this.context = context }.errorMsg
}

private fun Response<*>.getNetworkErrorMessage(context: Context): NetworkError {
    return when (code()) {
        NetworkError.TIMEOUT.code -> NetworkError.TIMEOUT
        NetworkError.TOO_MANY_REQUESTS.code -> {
            FirebaseCrashlytics.getInstance()
                .recordException(TsuHTTPClientException.newInstance(this))
            NetworkError.TOO_MANY_REQUESTS
        }
        NetworkError.BAD_REQUEST.code -> NetworkError.BAD_REQUEST
        NetworkError.UNAUTHORIZED.code -> NetworkError.UNAUTHORIZED
        NetworkError.FORBIDDEN.code -> NetworkError.FORBIDDEN
        else -> NetworkError.GENERIC
    }.apply { this.context = context }
}


inline fun <reified T> handleResponse(
    context: Context,
    response: Response<T>,
    onSuccess: (result: T) -> Unit,
    onFailure: (errMsg: String) -> Unit = {}
) {
    if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE) {
        Log.d("NetworkResponseHandler", response.body().toString())
        val body = response.body()
        if (body is DataWrapper<*>) {
            if (!body.error) {
                onSuccess(body)
            } else {
                onFailure(response.getNetworkCallErrorMessage(context))
            }
        } else {
            if (null is T) {//response body is allowed to be null
                onSuccess.invoke(response.body() as T)
            } else { // response body shouldn't be null
                if (response.body() != null) {
                    onSuccess.invoke(response.body()!!)
                } else {
                    onFailure.invoke(context.getString(R.string.generic_error_message))
                }
            }
        }
    } else {
        onFailure.invoke(response.getNetworkCallErrorMessage(context))
    }
}

class TsuHTTPClientException(message: String) : Exception(message) {
    companion object {
        fun newInstance(response: Response<*>): TsuHTTPClientException {
            val gson = Gson()
            val code = response.code()
            val url = response.raw().request.url
            val request = gson.toJson(response.raw().request.body)
            val responseBody = gson.toJson(response.body())
            val message =
                "HTTP call exception with code=$code for user with id= ${AuthenticationHelper.currentUserId} on url=$url \nRequestPayload = $request \nResponse = $responseBody"
            return TsuHTTPClientException(message)
        }
    }
}
