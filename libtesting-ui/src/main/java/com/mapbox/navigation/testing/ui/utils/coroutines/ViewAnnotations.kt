package com.mapbox.navigation.testing.ui.utils.coroutines

import android.view.View
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Waits until exactly [expectedCount] view annotations are shown.
 *
 * [ViewAnnotationManager] has no listener that fires exactly once per newly added view (its
 * update listener fires on position/visibility/anchor changes to already-attached views too), so
 * this polls the public `annotations` snapshot instead. `annotations` keeps every view ever
 * added, including ones later hidden (e.g. a callout view an adapter hides rather than removes),
 * so this filters by `isShown` rather than counting all attached views.
 */
suspend fun ViewAnnotationManager.awaitViewAnnotations(
    expectedCount: Int,
    timeout: Duration = 15.seconds,
): List<View> {
    var shown: List<View> = emptyList()
    withTimeout(timeout) {
        while (shown.size != expectedCount) {
            shown = annotations.keys.filter { it.isShown }
            delay(200.milliseconds)
        }
    }
    return shown
}
