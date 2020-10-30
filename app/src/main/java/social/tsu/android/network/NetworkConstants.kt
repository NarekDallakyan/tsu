package social.tsu.android.network

import java.util.concurrent.TimeUnit

object NetworkConstants {
    const val HTTP_SUCCESS_MIN = 200
    const val HTTP_SUCCESS_MAX = 299
    val HTTP_SUCCESS_RANGE = HTTP_SUCCESS_MIN .. HTTP_SUCCESS_MAX

    const val TIMEOUT = 30L
    const val IMAGE_UPLOAD_TIMEOUT = 90L
    val TIMEOUT_UNIT = TimeUnit.SECONDS

    const val COPYRIGHT_URL_FORMAT = "https://www.tsusocial.com/contact/intellectual-property?post_id=%d&firstname=%s&lastname=%s"
}
