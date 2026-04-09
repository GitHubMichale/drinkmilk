package com.example.drinkmilkapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.drinkmilkapp.data.FeedingStore
import com.example.drinkmilkapp.domain.FeedingChild
import com.example.drinkmilkapp.domain.FeedingRepository
import com.example.drinkmilkapp.service.FeedingForegroundService
import com.example.drinkmilkapp.ui.theme.DrinkMilkAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notifyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val repository = FeedingRepository(FeedingStore(applicationContext))
        FeedingForegroundService.start(this)

        setContent {
            DrinkMilkAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FeedingScreen(repository = repository)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun FeedingScreen(repository: FeedingRepository) {
    var child1Time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var child2Time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repository.initializeIfNeeded()
        repository.twinFeedingFlow.collect { (t1, t2) ->
            child1Time = t1
            child2Time = t2
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChildFeedingBlock(
            title = stringResource(R.string.child_1_label),
            lastFeedingMillis = child1Time,
            nowMillis = nowMillis,
            repository = repository,
            onMarkFed = {
                coroutineScope.launch {
                    repository.markFedNow(FeedingChild.CHILD_1)
                    FeedingForegroundService.requestRefresh(context)
                }
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        ChildFeedingBlock(
            title = stringResource(R.string.child_2_label),
            lastFeedingMillis = child2Time,
            nowMillis = nowMillis,
            repository = repository,
            onMarkFed = {
                coroutineScope.launch {
                    repository.markFedNow(FeedingChild.CHILD_2)
                    FeedingForegroundService.requestRefresh(context)
                }
            }
        )
    }
}

@Composable
private fun ChildFeedingBlock(
    title: String,
    lastFeedingMillis: Long,
    nowMillis: Long,
    repository: FeedingRepository,
    onMarkFed: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = repository.formatLastFeedingTime(lastFeedingMillis),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.elapsed_since,
                repository.formatElapsed(nowMillis - lastFeedingMillis)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onMarkFed) {
            Text(text = stringResource(R.string.action_mark_fed_for, title))
        }
    }
}
