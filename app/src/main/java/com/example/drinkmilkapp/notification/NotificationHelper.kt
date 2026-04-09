package com.example.drinkmilkapp.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.drinkmilkapp.MainActivity
import com.example.drinkmilkapp.R
import com.example.drinkmilkapp.domain.FeedingUiState
import com.example.drinkmilkapp.receiver.FeedActionReceiver

object NotificationHelper {
    const val CHANNEL_ID = "feeding_timer_channel"
    const val CHANNEL_NAME = "喂奶计时"
    const val NOTIFICATION_ID = 1001
    const val ACTION_MARK_FED = "com.example.drinkmilkapp.action.MARK_FED"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示喂奶时间和间隔"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(context: Context, uiState: FeedingUiState): Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val markFedPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, FeedActionReceiver::class.java).setAction(ACTION_MARK_FED),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("上次喂奶: ${uiState.lastFeedingTimeText}")
            .setContentText("已过 ${uiState.elapsedText}")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, context.getString(R.string.action_mark_fed), markFedPendingIntent)
            .build()
    }
}
