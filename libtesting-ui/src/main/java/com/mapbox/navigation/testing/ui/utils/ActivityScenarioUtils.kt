package com.mapbox.navigation.testing.ui.utils

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


suspend inline fun <reified T: Activity> withActivityScenario(block: suspend (ActivityScenario<T>) -> Unit) {
    val scenario = withContext(Dispatchers.Default) {
        ActivityScenario.launch(T::class.java)
    }
    try {
        block(scenario)
    } finally {
        withContext(Dispatchers.Default) {
            scenario.close()
        }
    }
}

suspend fun <T> ActivityScenario<T>.getActivity(): T where T : Activity {
    val scenario = this
    return suspendCoroutine { cont ->
        scenario.onActivity { activity ->
            cont.resume(activity)
        }
    }
}
