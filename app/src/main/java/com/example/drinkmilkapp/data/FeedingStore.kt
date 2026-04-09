package com.example.drinkmilkapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.drinkmilkapp.domain.FeedingChild
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "feeding_prefs")

class FeedingStore(private val context: Context) {
    private val legacyLastFeedingKey = longPreferencesKey("last_feeding_time")
    private val child1Key = longPreferencesKey("child1_last_feeding")
    private val child2Key = longPreferencesKey("child2_last_feeding")

    val twinLastFeedingMillisFlow: Flow<Pair<Long?, Long?>> = context.dataStore.data.map { prefs ->
        prefs[child1Key] to prefs[child2Key]
    }

    suspend fun ensureTwinInitialized() {
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val legacy = prefs[legacyLastFeedingKey]
            val c1 = prefs[child1Key]
            val c2 = prefs[child2Key]
            if (c1 == null && c2 == null && legacy != null) {
                prefs[child1Key] = legacy
                prefs[child2Key] = now
                prefs.remove(legacyLastFeedingKey)
                return@edit
            }
            if (c1 == null) prefs[child1Key] = now
            if (c2 == null) prefs[child2Key] = now
            if (prefs.contains(legacyLastFeedingKey)) {
                prefs.remove(legacyLastFeedingKey)
            }
        }
    }

    suspend fun setLastFeedingMillis(child: FeedingChild, value: Long) {
        context.dataStore.edit { prefs ->
            when (child) {
                FeedingChild.CHILD_1 -> prefs[child1Key] = value
                FeedingChild.CHILD_2 -> prefs[child2Key] = value
            }
        }
    }
}
