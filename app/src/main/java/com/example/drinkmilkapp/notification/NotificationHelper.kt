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
import com.example.drinkmilkapp.domain.FeedingChild
import com.example.drinkmilkapp.domain.TwinFeedingUiState
import com.example.drinkmilkapp.receiver.FeedActionReceiver

object NotificationHelper {
    const val CHANNEL_ID = "feeding_timer_channel"
    const val CHANNEL_NAME = "喂奶计时"
    const val NOTIFICATION_ID = 1001
    const val ACTION_MARK_FED = "com.example.drinkmilkapp.action.MARK_FED"
    const val EXTRA_FEEDING_CHILD = "extra_feeding_child"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示大宝、小宝的喂奶时间和间隔"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(context: Context, state: TwinFeedingUiState): Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val markFedChild1PendingIntent = PendingIntent.getBroadcast(
            context,
            10,
            Intent(context, FeedActionReceiver::class.java)
                .setAction(ACTION_MARK_FED)
                .putExtra(EXTRA_FEEDING_CHILD, FeedingChild.CHILD_1.ordinal),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val markFedChild2PendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, FeedActionReceiver::class.java)
                .setAction(ACTION_MARK_FED)
                .putExtra(EXTRA_FEEDING_CHILD, FeedingChild.CHILD_2.ordinal),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val line1 = context.getString(
            R.string.notif_line_child,
            context.getString(R.string.child_1_label),
            state.child1.lastFeedingTimeText,
            state.child1.elapsedText
        )
        val line2 = context.getString(
            R.string.notif_line_child,
            context.getString(R.string.child_2_label),
            state.child2.lastFeedingTimeText,
            state.child2.elapsedText
        )
        val bigText = "$line1\n$line2"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(line1)
            .setContentText(line1)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                0,
                context.getString(R.string.action_mark_fed_child_1),
                markFedChild1PendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.action_mark_fed_child_2),
                markFedChild2PendingIntent
            )
            .build()
    }
}
