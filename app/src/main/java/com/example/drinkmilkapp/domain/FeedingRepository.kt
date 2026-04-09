package com.example.drinkmilkapp.domain

import com.example.drinkmilkapp.data.FeedingStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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

class FeedingRepository(private val store: FeedingStore) {
    val lastFeedingTimeFlow: Flow<Long> = store.lastFeedingTimeMillisFlow.map { value ->
        value ?: System.currentTimeMillis()
    }

    suspend fun initializeIfNeeded() {
        val current = store.lastFeedingTimeMillisFlow.firstOrNull()
        if (current == null) {
            store.setLastFeedingTimeMillis(System.currentTimeMillis())
        }
    }

    suspend fun markFedNow() {
        store.setLastFeedingTimeMillis(System.currentTimeMillis())
    }

    fun buildUiState(lastFeedingTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): FeedingUiState {
        return FeedingUiState(
            lastFeedingTimeMillis = lastFeedingTimeMillis,
            lastFeedingTimeText = formatLastFeedingTime(lastFeedingTimeMillis),
            elapsedText = formatElapsed(nowMillis - lastFeedingTimeMillis)
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
