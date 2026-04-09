package com.example.drinkmilkapp.domain

import com.example.drinkmilkapp.data.FeedingStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class FeedingUiState(
    val lastFeedingTimeMillis: Long,
    val lastFeedingTimeText: String,
    val elapsedText: String
)

data class TwinFeedingUiState(
    val child1: FeedingUiState,
    val child2: FeedingUiState
)

class FeedingRepository(private val store: FeedingStore) {
    val twinFeedingFlow: Flow<Pair<Long, Long>> = store.twinLastFeedingMillisFlow.map { (c1, c2) ->
        val now = System.currentTimeMillis()
        (c1 ?: now) to (c2 ?: now)
    }

    suspend fun initializeIfNeeded() {
        store.ensureTwinInitialized()
    }

    suspend fun markFedNow(child: FeedingChild) {
        store.setLastFeedingMillis(child, System.currentTimeMillis())
    }

    fun buildUiState(lastFeedingTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): FeedingUiState {
        return FeedingUiState(
            lastFeedingTimeMillis = lastFeedingTimeMillis,
            lastFeedingTimeText = formatLastFeedingTime(lastFeedingTimeMillis),
            elapsedText = formatElapsed(nowMillis - lastFeedingTimeMillis)
        )
    }

    fun buildTwinUiState(
        child1Millis: Long,
        child2Millis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): TwinFeedingUiState {
        return TwinFeedingUiState(
            child1 = buildUiState(child1Millis, nowMillis),
            child2 = buildUiState(child2Millis, nowMillis)
        )
    }

    fun formatLastFeedingTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    fun formatElapsed(diffMillis: Long): String {
        val safeDiff = diffMillis.coerceAtLeast(0)
        val hours = TimeUnit.MILLISECONDS.toHours(safeDiff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeDiff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeDiff) % 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分${seconds}秒"
            minutes > 0 -> "${minutes}分${seconds}秒"
            else -> "${seconds}秒"
        }
    }
}
