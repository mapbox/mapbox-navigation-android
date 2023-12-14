package com.mapbox.navigation.qa_test_app.view.util

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.utils.internal.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object Utils {
    /**
     * Returns the Mapbox access token set in the app resources.
     *
     * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
     * @return The Mapbox access token or null if not found.
     */
    fun getMapboxAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_access_token", "string", context.packageName)
        return if (tokenResId != 0) context.getString(tokenResId) else ""
    }
}

internal inline fun <T> Flow<T>.observe(
    lifecycleOwner: LifecycleOwner,
    crossinline action: suspend (value: T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect(action)
        }
    }
}
