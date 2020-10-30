package social.tsu.android

import android.app.Application
import okhttp3.OkHttpClient

interface Flipper {
    fun addInterceptors(okHttpBuilder: OkHttpClient.Builder)
    fun init(application: Application)
}

