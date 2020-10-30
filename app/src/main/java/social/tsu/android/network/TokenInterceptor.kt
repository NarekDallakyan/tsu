package social.tsu.android.network

import android.app.Application
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.AuthenticationApi
import social.tsu.android.network.model.LoginRequest
import social.tsu.android.utils.AppVersion
import javax.inject.Inject

class TokenInterceptor @Inject constructor(private val application: Application,
                                           private val authenticationApi: AuthenticationApi,
                                           private val encryptedSharePrefs:SharedPreferences,
                                           private val apiKey:String): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        synchronized(this) {
            val originalRequest = chain.request()
            val authorizedUrl = originalRequest.url.newBuilder().addQueryParameter("access_token",AuthenticationHelper.authToken).build()
            val authenticationRequest = originalRequest.newBuilder()
                .addHeader("x-api-key", apiKey)
                .url(authorizedUrl)
                .build()

            val initialResponse = chain.proceed(authenticationRequest)

            if (initialResponse.code == 403 || initialResponse.code == 401) { // unauthorized response from server

                val username = encryptedSharePrefs.getString(AuthenticationHelper.KEY_USERNAME,"")!!
                val password = encryptedSharePrefs.getString(AuthenticationHelper.KEY_PASSWORD,"")!!

                if(username.isBlank() && password.isBlank()){
                    //credentials already invalidated
                    return initialResponse
                }else if (username.isBlank() || password.isBlank() ){
                    //
                    invalidateCredentialsAndGoToLogin()
                    return initialResponse
                }

                //refresh user access token
                val refreshTokenResponse =

                    authenticationApi.refreshToken(
                        LoginRequest(
                            username,
                            password,
                            "device",
                            AppVersion.versionNameCodeConcat
                        )
                    )
                        .execute()

                when {
                    refreshTokenResponse.code() != 200 -> {
                        // unsuccessful token refresh push user to login
                        invalidateCredentialsAndGoToLogin()
                        return initialResponse
                    }
                    else -> {
                        // token refresh was successfull. Retry previous api call with new token
                        val data = refreshTokenResponse.body()?.data
                        AuthenticationHelper.update(data)
                        val newAuthorizedUrl = originalRequest
                            .url
                            .newBuilder()
                            .addQueryParameter("access_token", AuthenticationHelper.authToken)
                            .build()

                        val newAuthenticationRequest = authenticationRequest.newBuilder()
                            .url(newAuthorizedUrl)
                            .build()

                        return chain.proceed(newAuthenticationRequest)
                    }
                }
            }
            else return initialResponse
        }

    }
    private fun invalidateCredentialsAndGoToLogin(){

        // invalidate persisted login details
        encryptedSharePrefs.edit()?.apply {
            remove("LOGIN_USER")
            remove("LOGIN_PASS")
            apply()
        }
        AuthenticationHelper.clear()

        //go to login screen
        AuthenticationHelper.goToLogin(application)
    }
}