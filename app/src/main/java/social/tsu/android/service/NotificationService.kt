package social.tsu.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import social.tsu.android.R
import javax.inject.Inject

class NotificationService @Inject constructor(
    private val notificationManager: NotificationManager,
    private val notificationBuilder: NotificationCompat.Builder
) {

    companion object {
        const val NOTIFICATION_NAME = "General"
        const val OLD_NOTIFICATION_CHANNEL = "tasks_channel"
        const val NOTIFICATION_CHANNEL = "general_channel"
    }

    fun sendProgressNotification(notificationId: Int, drawable: Drawable, text: String,
                                 current: Int = 1, max: Int = 100) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_HIGH

            )

            channel.enableVibration(false)
            channel.setSound(null, null)

            notificationManager.deleteNotificationChannel(OLD_NOTIFICATION_CHANNEL)
            notificationManager.createNotificationChannel(channel)
        }

        val bigPictureStyle = NotificationCompat.BigPictureStyle()
            .setSummaryText(text)
            .bigPicture(drawable.toBitmap())

        val priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        }else {
            Notification.PRIORITY_MAX
        }

        val notification = notificationBuilder
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_tsu_logo)
            .setPriority(priority)
            .setChannelId(NOTIFICATION_CHANNEL)
            .setStyle(bigPictureStyle)
            .setProgress(max, current, false)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}

