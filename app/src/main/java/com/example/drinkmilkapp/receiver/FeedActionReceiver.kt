package com.example.drinkmilkapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.drinkmilkapp.data.FeedingStore
import com.example.drinkmilkapp.domain.FeedingChild
import com.example.drinkmilkapp.domain.FeedingRepository
import com.example.drinkmilkapp.notification.NotificationHelper
import com.example.drinkmilkapp.service.FeedingForegroundService
import com.example.drinkmilkapp.widget.DualFeedingWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotificationHelper.ACTION_MARK_FED) return
        val ordinal = intent.getIntExtra(NotificationHelper.EXTRA_FEEDING_CHILD, -1)
        val child = FeedingChild.entries.getOrNull(ordinal) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val appContext = context.applicationContext
            val repo = FeedingRepository(FeedingStore(appContext))
            repo.markFedNow(child)
            FeedingForegroundService.requestRefresh(appContext)
            DualFeedingWidgetProvider.requestUpdate(appContext)
            pendingResult.finish()
        }
    }
}
