package social.tsu.android.network.api

import android.util.Log
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import social.tsu.android.di.NetworkModule
import social.tsu.android.network.CountingRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named


class UploadVideoService @Inject constructor(@Named(NetworkModule.NAMED_OKHTTP_CLIENT_DEFAULT) private val okHttpClient: OkHttpClient){

    fun uploadVideoFile(id: String, uploadUrl: String,
                        videoFile: File, progressSubject: PublishSubject<Int>): Single<Response> {
        Log.d("UploadTask", "Preparing to upload [$id] $videoFile to $uploadUrl")
        val mediaType = "video".toMediaTypeOrNull()
        val fileRequestBody = videoFile.asRequestBody(mediaType)

        return uploadRequest(fileRequestBody, progressSubject, uploadUrl)
    }

    private fun uploadRequest(fileRequestBody: RequestBody,
                              progressSubject: PublishSubject<Int>,
                              uploadUrl: String
    ): Single<Response> {
        val requestBody = CountingRequestBody
            .createUploadRequestBody(fileRequestBody, progressSubject)

        val request = Request.Builder()
            .header("Content-type", "video")
            .url(uploadUrl)
            .put(requestBody)
            .build()


        return Single.create<Response> {
            val call = okHttpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if(!it.isDisposed) it.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!it.isDisposed) it.onSuccess(response)
                }
            })
        }
    }

}