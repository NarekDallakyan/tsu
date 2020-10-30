package social.tsu.android.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.getSystemService
import com.google.android.exoplayer2.SimpleExoPlayer


fun Context.isWifiConnected(): Boolean {
    val connectivityManager = getSystemService<ConnectivityManager>() ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}

fun Context.createAppExoPlayer(): SimpleExoPlayer {
    return SimpleExoPlayer.Builder(this)
        .setUseLazyPreparation(true)
        .build()
}

fun Context.openUrl(link: String?) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(
            "ContextExt#openUrl",
            "Unable to start link $link because no available app on device",
            e
        )
    } catch (e: Exception) {
        Log.e("ContextExt#openUrl", "Unable to start link $link", e)
    }
}

fun Context.getPickIntent(type: String): Intent {
    val getIntent = Intent(Intent.ACTION_GET_CONTENT)
    getIntent.type = type

    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    pickIntent.type = type

    val chooserIntent = Intent.createChooser(getIntent, "Select Image")
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
    return chooserIntent
}