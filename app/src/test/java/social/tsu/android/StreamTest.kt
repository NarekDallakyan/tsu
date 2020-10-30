package social.tsu.android

import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import social.tsu.android.di.NetworkModule
import social.tsu.android.network.api.StreamApi

class StreamTest {
    lateinit var streamApi: StreamApi

//    private val mockWebServer = MockWebServer()

    @Before
    fun setup(){
//        mockWebServer.start()

        val flipper = FlipperConfig()
        val networkModule = NetworkModule()

        streamApi = networkModule.run {
            provideStreamApi(provideokHttpClient(provideApiGatewayInterceptor(), flipper), provideMoshi())
        }

    }

//    @After
//    fun teardown() {
//        mockWebServer.shutdown()
//    }

    @Test
    fun testCreateStream(){
        val testObserver = streamApi.createStream("faf68f32522c4de393d0543b150b9b4c").test()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        val response = testObserver.values()[0]

        Truth.assertThat(response.code()).isEqualTo(201)
        Truth.assertThat(response.body()).isNotNull()

        val streamResponse = response.body()!!.data.stream
        Truth.assertThat(streamResponse.id).isNotNull()
    }
}

