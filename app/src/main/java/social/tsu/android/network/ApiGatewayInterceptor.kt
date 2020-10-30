package social.tsu.android.network

import okhttp3.Interceptor
import okhttp3.Response
import social.tsu.android.network.api.Environment

class ApiGatewayInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(this) {
            val request = chain.request()

            val authenticatedRequest = request.newBuilder()
                .addHeader("x-api-key", Environment.key)
                .build()

            return chain.proceed(authenticatedRequest)
        }
    }
}