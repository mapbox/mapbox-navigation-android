package com.mapbox.navigation.dropin.backpress

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.endNavigation
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Back pressed callback for the [NavigationView].
 *
 * On back pressed will update the navigation state.
 *
 * (FreeDrive) <- (DestinationPreview) <- (RoutePreview) <- (ActiveNavigation)
 *             <- (Arrival)
 */
internal class BackPressedComponent(
    onBackPressedDispatcher: OnBackPressedDispatcher,
    private val store: Store,
    private val lifecycleOwner: LifecycleOwner,
) : UIComponent() {

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {

        override fun handleOnBackPressed() {
            when (store.state.value.navigation) {
                NavigationState.FreeDrive -> {
                    logE(
                        msg = "The back pressed callback should be disabled in Free Drive state",
                        category = "BackPressedComponent",
                    )
                }
                NavigationState.DestinationPreview -> {
                    store.dispatch(DestinationAction.SetDestination(null))
                    store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
                }
                NavigationState.RoutePreview -> {
                    store.dispatch(RoutePreviewAction.Ready(emptyList()))
                    store.dispatch(NavigationStateAction.Update(NavigationState.DestinationPreview))
                }
                NavigationState.ActiveNavigation -> {
                    store.dispatch(RoutesAction.SetRoutes(emptyList()))
                    store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
                }
                NavigationState.Arrival -> {
                    store.dispatch(endNavigation())
                }
            }
        }
    }

    // Add the callback before onAttached so downstream developers can register higher priority
    // back press callbacks. This means the component has to be registered via attachCreated.
    init {
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.select { it.navigation != NavigationState.FreeDrive }
                    .onCompletion { onBackPressedCallback.isEnabled = false }
                    .collect { onBackPressedCallback.isEnabled = it }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        onBackPressedCallback.remove()
    }
}

suspend fun Lifecycle.repeatOnLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    require(state !== Lifecycle.State.INITIALIZED) {
        "repeatOnLifecycle cannot start work with the INITIALIZED lifecycle state."
    }

    if (currentState === Lifecycle.State.DESTROYED) {
        return
    }

    coroutineScope {
        withContext(Dispatchers.Main.immediate) {
            // Check the current state of the lifecycle as the previous check is not guaranteed
            // to be done on the main thread.
            if (currentState === Lifecycle.State.DESTROYED) return@withContext

            // Instance of the running repeating coroutine
            var launchedJob: Job? = null

            // Registered observer
            var observer: LifecycleEventObserver? = null

            try {
                // Suspend the coroutine until the lifecycle is destroyed or
                // the coroutine is cancelled
                suspendCancellableCoroutine<Unit> { cont ->
                    // Lifecycle observers that executes `block` when the lifecycle reaches certain state, and
                    // cancels when it moves falls below that state.
                    val startWorkEvent = Lifecycle.Event.upTo(state)
                    val cancelWorkEvent = Lifecycle.Event.downFrom(state)
                    observer = LifecycleEventObserver { _, event ->
                        if (event == startWorkEvent) {
                            // Launch the repeating work preserving the calling context
                            launchedJob = this@coroutineScope.launch(block = block)
                            return@LifecycleEventObserver
                        }
                        if (event == cancelWorkEvent) {
                            launchedJob?.cancel()
                            launchedJob = null
                        }
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            cont.resume(Unit)
                        }
                    }
                    this@repeatOnLifecycle.addObserver(observer as LifecycleEventObserver)
                }
            } finally {
                launchedJob?.cancel()
                observer?.let {
                    this@repeatOnLifecycle.removeObserver(it)
                }
            }
        }
    }
}
