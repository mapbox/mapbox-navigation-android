package com.mapbox.navigation.instrumentation_tests.utils

import androidx.test.core.app.ActivityScenario
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.activity.NavigationStateVisualizationActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun withVisualization(
    title: String = "test visualisation",
    block: suspend () -> Unit,
) {
    if (!MapboxNavigationProvider.isCreated()) {
        error("withVisualization requires mapbox navigation to be created")
    }
    withContext(Dispatchers.Default) {
        val scenario = ActivityScenario.launch(NavigationStateVisualizationActivity::class.java)
        suspendCoroutine<Unit> { continuation ->
            scenario.onActivity {
                it.supportActionBar?.setTitle(title)
                continuation.resume(Unit)
            }
        }
        scenario.use { _ ->
            withContext(Dispatchers.Main) {
                block()
            }
        }
    }
}
