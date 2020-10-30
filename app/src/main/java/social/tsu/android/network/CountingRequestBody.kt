package social.tsu.android.network

import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer
import java.io.IOException
import kotlin.math.roundToInt

typealias CountingRequestListener = (bytesWritten: Long, contentLength: Long) -> Unit
class CountingRequestBody(private val requestBody: RequestBody,
                          private val onProgressUpdate: CountingRequestListener
): RequestBody() {
    override fun contentType(): MediaType? {
        return requestBody.contentType()
    }

    override fun contentLength(): Long  = requestBody.contentLength()

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(
            sink,
            this,
            onProgressUpdate
        )
        val bufferedSink = countingSink.buffer()

        requestBody.writeTo(bufferedSink)

        bufferedSink.flush()
    }

    companion object {
        fun createUploadRequestBody(
            requestBody: RequestBody,
            progressEmitter: PublishSubject<Int>
        ): RequestBody {
            return CountingRequestBody(requestBody) { bytesWritten, contentLength ->
                val progress = (1.0 * bytesWritten / contentLength).roundToInt()
                progressEmitter.onNext(progress)

                if (progress >= 1.0) {
                    progressEmitter.onComplete()
                }
            }
        }
    }
}
