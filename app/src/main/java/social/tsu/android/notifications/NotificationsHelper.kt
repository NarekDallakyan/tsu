package social.tsu.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import social.tsu.android.R
import social.tsu.android.data.local.models.TsuNotificationType
import social.tsu.android.ui.MainActivity

object NotificationsHelper{

    fun buildNotification(context: Context, message: String, largeIcon:Bitmap?, type: TsuNotificationType) {

        initChannels(context,type)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.tsu_white)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setLargeIcon(largeIcon)
            .setContentIntent(createNotificationIntent(context))
            .setAutoCancel(true)
            .build()

        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)

    }

    private fun createNotificationIntent(context: Context): PendingIntent {

        return NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.notificationFragment)
            .createPendingIntent()
    }

    private fun initChannels(context: Context, type: TsuNotificationType) {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelName = context.getString(R.string.notification_channel_name)
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    private const val NOTIFICATION_CHANNEL_ID = "Tsu Notification"

    private const val NOTIFICATION_ID = 1001


}