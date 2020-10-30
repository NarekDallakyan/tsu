package social.tsu.android

import junit.framework.Assert.assertTrue
import junit.framework.Assert.fail
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import social.tsu.android.network.api.AuthenticationApi
import social.tsu.android.network.model.LoginRequest
import social.tsu.android.rx.TestSchedulers
import social.tsu.android.service.AuthenticationService
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class AuthenticationServiceTest {


    private val mockWebServer = MockWebServer()

    private lateinit var authenticationApi: AuthenticationApi

    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @Before
    fun setup() {
        mockWebServer.start()

        authenticationApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .client(okHttpClient)
            .build()
            .create(AuthenticationApi::class.java)


    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testSuccessfullLogin() {

        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(success)
        mockWebServer.enqueue(response)

        val authService = AuthenticationService(authenticationApi, TestSchedulers())

        val loginRequest = LoginRequest("username", "password", "deviceid")
        authService.authenticate(loginRequest, {
            assertTrue(true)
        },
            {
                fail()

            })

        Thread.sleep(2000)
    }

    @Test
    fun testFailLogin() {

        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_FORBIDDEN)
            .setBody("")
        mockWebServer.enqueue(response)

        val authService = AuthenticationService(authenticationApi, TestSchedulers())

        val loginRequest = LoginRequest("username", "password", "deviceid")
        authService.authenticate(loginRequest, {
            fail()
        },
            {
                assertTrue(true)

            })


        Thread.sleep(2000)
    }

    @Test
    fun testServerError() {

        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
            .setBody("")
        mockWebServer.enqueue(response)

        val authService = AuthenticationService(authenticationApi, TestSchedulers())

        val loginRequest = LoginRequest("username", "password", "deviceid")
        authService.authenticate(loginRequest, {
            fail()
        },
            {
                assertTrue(true)

            })


        Thread.sleep(2000)
    }

    val success: String = "{\n" +
            "    \"data\": {\n" +
            "        \"id\": 2,\n" +
            "        \"username\": \"test_admin\",\n" +
            "        \"email\": \"admin@tsu.co\",\n" +
            "        \"auth_token\": \"f6ac3c2c05ae66dda2b182641d44452a\",\n" +
            "        \"full_name\": \"Admin User\",\n" +
            "        \"profile_picture_url\": \"/assets/user.png\",\n" +
            "        \"cover_picture_url\": \"/cover_pictures/original/missing.png\",\n" +
            "        \"is_friend\": false,\n" +
            "        \"friendship_status\": null,\n" +
            "        \"is_following\": false,\n" +
            "        \"friend_count\": 0,\n" +
            "        \"follower_count\": 3,\n" +
            "        \"following_count\": 3,\n" +
            "        \"gender\": \"Male\",\n" +
            "        \"birthday\": \"\",\n" +
            "        \"is_birthday_private\": false,\n" +
            "        \"verified_status\": 1,\n" +
            "        \"accept_friend_request\": true,\n" +
            "        \"is_wall_private\": true,\n" +
            "        \"role\": \"admin\",\n" +
            "        \"phone_number\": \"\"\n" +
            "    }\n" +
            "}"
}

