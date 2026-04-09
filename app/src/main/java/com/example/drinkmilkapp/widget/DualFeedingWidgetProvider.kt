package com.example.drinkmilkapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.drinkmilkapp.R
import com.example.drinkmilkapp.data.FeedingStore
import com.example.drinkmilkapp.domain.FeedingChild
import com.example.drinkmilkapp.domain.FeedingRepository
import com.example.drinkmilkapp.notification.NotificationHelper
import com.example.drinkmilkapp.receiver.FeedActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DualFeedingWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        scope.cancel()
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repo = FeedingRepository(FeedingStore(context.applicationContext))
        scope.launch {
            repo.initializeIfNeeded()
            val (t1, t2) = repo.twinFeedingFlow.first()
            val now = System.currentTimeMillis()
            val s1 = repo.buildUiState(t1, now)
            val s2 = repo.buildUiState(t2, now)

            appWidgetIds.forEach { id ->
                val views = buildRemoteViews(context, s1.lastFeedingTimeText, s1.elapsedText, s2.lastFeedingTimeText, s2.elapsedText)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        child1Last: String,
        child1Elapsed: String,
        child2Last: String,
        child2Elapsed: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_dual_feeding)
        views.setTextViewText(R.id.widget_child1_last, child1Last)
        views.setTextViewText(R.id.widget_child1_elapsed, context.getString(R.string.elapsed_since, child1Elapsed))
        views.setTextViewText(R.id.widget_child2_last, child2Last)
        views.setTextViewText(R.id.widget_child2_elapsed, context.getString(R.string.elapsed_since, child2Elapsed))

        views.setOnClickPendingIntent(
            R.id.widget_child1_button,
            buildMarkFedPendingIntent(context, FeedingChild.CHILD_1)
        )
        views.setOnClickPendingIntent(
            R.id.widget_child2_button,
            buildMarkFedPendingIntent(context, FeedingChild.CHILD_2)
        )

        return views
    }

    private fun buildMarkFedPendingIntent(context: Context, child: FeedingChild): PendingIntent {
        val intent = Intent(context, FeedActionReceiver::class.java)
            .setAction(NotificationHelper.ACTION_MARK_FED)
            .putExtra(NotificationHelper.EXTRA_FEEDING_CHILD, child.ordinal)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val requestCode = when (child) {
            FeedingChild.CHILD_1 -> 100
            FeedingChild.CHILD_2 -> 101
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    companion object {
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DualFeedingWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val provider = DualFeedingWidgetProvider()
                provider.updateAllWidgets(context, manager, ids)
            }
        }
    }
}

