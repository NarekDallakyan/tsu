package social.tsu.android.helper

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import social.tsu.android.R
import social.tsu.android.ui.snack


object NetworkHelper {

    @Suppress("DEPRECATION")
    fun isNetworkAvailable(context: Context): Boolean {
        var result = false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                        else -> false
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        result = true
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        result = true
                    }
                }
                cm.activeNetworkInfo?.let {
                    result = it.isConnected
                }
            }
        }
        return result
    }


    fun openInternetDialog(activity: Activity) {
        activity.snack(activity.getString(R.string.connectivity_issues_message))
    }

    fun isOnline(): Boolean {
        try {
            val p1 = Runtime.getRuntime().exec("ping -c 1 www.google.com")
            val returnVal = p1.waitFor()
            return returnVal == 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
