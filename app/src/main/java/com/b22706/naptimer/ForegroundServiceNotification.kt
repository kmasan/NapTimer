package com.b22706.naptimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat


// 通知
// https://developer.android.com/guide/topics/ui/notifiers/notifications
internal object ForegroundServiceNotification {
    const val FOREGROUND_SERVICE_NOTIFICATION_ID: Int = 100
    private const val CHANNEL_ID = "channel_id"
    private const val CHANNEL_NAME = R.string.app_name.toString()
    fun createNotificationChannel(context: Context): NotificationManager {
        // 通知チャネルの作成と管理
        // https://developer.android.com/training/notify-user/channels

        // Android 8.0 以降、すべての通知をチャンネルに割り当てる必要がある
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return manager //すでに作成済みの場合など
        }

        // Foreground Service の通知の場合、IMPORTANCE_MIN は非推奨
        // https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_MIN

        // Foreground Service の通知の場合、IMPORTANCE_HIGH 未満を指定しても、IMPORTANCE_HIGH として扱われている？
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableLights(false)
            enableVibration(false)
        }
        // 重要度ごとの違い
        // https://developer.android.com/training/notify-user/channels#importance

        //すでに指定したチャンネルが作成済みの場合、何も起きない
        manager.createNotificationChannel(notificationChannel)

        return manager
    }

    fun createServiceNotification(
        context: Context,
        pendingIntent: PendingIntent,
        sendPendingIntent:PendingIntent,
        text: String
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle("サービス稼働中")
            setContentText(text)
            setContentIntent(pendingIntent)
            addAction(R.drawable.ic_launcher_foreground, "停止する", sendPendingIntent)
        }.build()

    fun createCustomNotification(
        context: Context,
        notificationLayout: RemoteViews,
        notificationLayoutExpanded: RemoteViews
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setCustomContentView(notificationLayout)
            setCustomBigContentView(notificationLayoutExpanded)
            //setCustomHeadsUpContentView(notificationLayoutExpanded)
        }.build()
}