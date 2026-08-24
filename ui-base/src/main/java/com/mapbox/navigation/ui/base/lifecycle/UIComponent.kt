package com.mapbox.navigation.ui.base.lifecycle

import androidx.annotation.CallSuper
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Using the [UIComponent] gives you access to a [coroutineScope] which uses
 * `Dispatchers.Main.immediate`. All coroutines that you
 * launch inside [onAttached] will be canceled when the observer is detached. You do not need to
 * implement onDetached for your flowable components.
 *
 * @property coroutineScope Defines a scope for new coroutine
 */
open class UIComponent : MapboxNavigationObserver {

    lateinit var coroutineScope: CoroutineScope

    /**
     * Signals that the [mapboxNavigation] instance is attached and ready for use.
     * @param mapboxNavigation
     */
    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     * @param mapboxNavigation
     */
    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        coroutineScope.cancel()
    }

    protected inline fun <T> Flow<T>.observe(
        context: CoroutineContext = EmptyCoroutineContext,
        crossinline action: suspend (value: T) -> Unit,
    ) {
        coroutineScope.launch(context) { collect { action(it) } }
    }
}
