package com.mapbox.navigation.ui.androidauto.search

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.dispatchers.SdkDispatchers
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequest
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequestCallback
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import kotlinx.coroutines.launch

/**
 * Route preview callback for a screen that requests routes for a selected place.
 *
 * A route request can finish after its screen stopped being the visible one, for example when
 * the driver opened another screen from the action strip in the meantime. Acting on the result
 * at that moment would push the route preview on top of, or go back from, a screen the request
 * has nothing to do with. Results are therefore applied only while the [lifecycle] is
 * [Lifecycle.State.RESUMED]. A result that arrives earlier is kept, with the latest one winning,
 * and applied after the screen is resumed again. It is dropped if the shared route preview shows
 * a different place by then, because another request finished in the meantime. When the screen
 * is destroyed, the kept result is dropped and the request made with this callback is cancelled.
 *
 * @param onError shows the given message to the driver.
 */
@UiThread
internal class ResumedRoutePreviewCallback(
    private val lifecycle: Lifecycle,
    private val mapboxScreenManager: MapboxScreenManager,
    private val routePreviewRequest: CarRoutePreviewRequest,
    private val onError: (messageRes: Int) -> Unit,
) : CarRoutePreviewRequestCallback {

    /**
     * A result kept while the screen was not resumed, with the place the shared route preview
     * showed at that moment.
     */
    private class Pending(val previewedPlace: PlaceRecord?, val action: () -> Unit)

    private var pending: Pending? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                @OptIn(MapboxExperimental::class)
                override fun onResume(owner: LifecycleOwner) {
                    if (pending == null) return
                    // ON_RESUME is often dispatched from inside a screen change, for example the
                    // ScreenManager.pop() of MapboxScreenManager.goBack(). Changing screens here
                    // would nest a second change inside that one, so apply the result from a
                    // posted task instead. The non-immediate Main dispatcher always posts.
                    lifecycle.coroutineScope.launch(SdkDispatchers.Main) { applyPending() }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    pending = null
                    routePreviewRequest.cancelRequest(this@ResumedRoutePreviewCallback)
                }
            },
        )
    }

    /**
     * Shows [messageRes] to the driver under the same rule as route results: now if the screen is
     * resumed, otherwise once it is resumed again.
     */
    fun showError(@StringRes messageRes: Int) {
        deliverError(messageRes)
    }

    override fun onRoutesReady(placeRecord: PlaceRecord, routes: List<NavigationRoute>) {
        deliver {
            if (
                !mapboxScreenManager.isScreenBelowTop(MapboxScreen.NAVIGATION) ||
                !mapboxScreenManager.goBack()
            ) {
                // The search and places screens belong to the legacy flow, which shows the
                // preview as its own ROUTE_PREVIEW screen rather than inside NAVIGATION.
                @Suppress("DEPRECATION")
                MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW)
            }
        }
    }

    override fun onUnknownCurrentLocation() {
        deliverError(R.string.car_search_unknown_current_location)
    }

    override fun onDestinationLocationUnknown() {
        deliverError(R.string.car_search_unknown_search_location)
    }

    override fun onNoRoutesFound() {
        deliverError(R.string.car_search_no_results)
    }

    override fun onNetworkFailure() {
        deliverError(R.string.car_search_error)
    }

    override fun onRoutingFailure(reasons: List<RouterFailure>) {
        deliverError(R.string.car_search_error)
    }

    private fun deliverError(@StringRes messageRes: Int) {
        deliver { onError(messageRes) }
    }

    private fun deliver(action: () -> Unit) {
        when {
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> action()
            lifecycle.currentState == Lifecycle.State.DESTROYED -> Unit
            else -> pending = Pending(previewedPlace(), action)
        }
    }

    private fun applyPending() {
        // The screen may have been paused again before the posted task ran; keep the result then.
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val kept = pending ?: return
        pending = null
        // Another request finished while this screen was hidden, so this result is stale: a kept
        // route would preview the other place, and a kept error would contradict its preview.
        if (previewedPlace() != kept.previewedPlace) return
        kept.action()
    }

    private fun previewedPlace(): PlaceRecord? = routePreviewRequest.repository?.placeRecord?.value
}
