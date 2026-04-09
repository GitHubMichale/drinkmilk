package com.example.drinkmilkapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.example.drinkmilkapp.data.FeedingStore
import com.example.drinkmilkapp.domain.FeedingRepository
import com.example.drinkmilkapp.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FeedingForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: FeedingRepository
    private var refreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = FeedingRepository(FeedingStore(applicationContext))
        NotificationHelper.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> refreshNotification()
            else -> startForegroundAndLoop()
        }
        return START_STICKY
    }

    private fun startForegroundAndLoop() {
        serviceScope.launch {
            repository.initializeIfNeeded()
            val lastTime = repository.lastFeedingTimeFlow.first()
            val uiState = repository.buildUiState(lastTime)
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                NotificationHelper.buildNotification(this@FeedingForegroundService, uiState)
            )
        }

        if (refreshJob == null) {
            refreshJob = serviceScope.launch {
                while (isActive) {
                    refreshNotificationSuspend()
                    delay(60_000)
                }
            }
        }
    }

    private fun refreshNotification() {
        serviceScope.launch { refreshNotificationSuspend() }
    }

    private suspend fun refreshNotificationSuspend() {
        repository.initializeIfNeeded()
        val lastTime = repository.lastFeedingTimeFlow.first()
        val uiState = repository.buildUiState(lastTime)
        NotificationManagerCompat.from(this@FeedingForegroundService).notify(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildNotification(this@FeedingForegroundService, uiState)
        )
    }

    override fun onDestroy() {
        refreshJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_REFRESH = "com.example.drinkmilkapp.action.REFRESH"

        fun start(context: Context) {
            val intent = Intent(context, FeedingForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun requestRefresh(context: Context) {
            val intent = Intent(context, FeedingForegroundService::class.java).apply {
                action = ACTION_REFRESH
            }
            context.startService(intent)
        }
    }
}
