package social.tsu.android.network

import android.content.Context
import androidx.annotation.StringRes
import social.tsu.android.R

data class NetworkError(val code: Int, @StringRes val errorMsgRes: Int) {
    var context: Context? = null
    var errorMsg: String = ""
        get() = context?.getString(errorMsgRes) ?: field

    companion object {
        val TOO_MANY_REQUESTS = NetworkError(429, R.string.four_twenty_nine_error_message)
        val TIMEOUT = NetworkError(408, R.string.connectivity_issues_message)
        val BAD_REQUEST = NetworkError(400, R.string.generic_error_message)
        val UNAUTHORIZED = NetworkError(401, R.string.unauthorized_message)
        val FORBIDDEN = NetworkError(403, R.string.generic_error_message)
        val GENERIC = NetworkError(1000, R.string.generic_error_message)
    }
}