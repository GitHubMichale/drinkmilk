package com.example.drinkmilkapp.data

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "feeding_prefs")

class FeedingStore(private val context: Context) {
    private val lastFeedingKey = longPreferencesKey("last_feeding_time")

    val lastFeedingTimeMillisFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[lastFeedingKey]
    }

    suspend fun setLastFeedingTimeMillis(value: Long) {
        context.dataStore.edit { prefs ->
            prefs[lastFeedingKey] = value
        }
    }
}
