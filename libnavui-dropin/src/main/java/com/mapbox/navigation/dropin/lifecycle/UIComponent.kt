package com.mapbox.navigation.dropin.lifecycle

import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Using the [UIComponent] gives you access to a [coroutineScope]. All coroutines that you
 * launch inside onAttached will be canceled when the observer is detached. You do not need to
 * implement onDetached for your flowable components.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
open class UIComponent : MapboxNavigationObserver {

    lateinit var coroutineScope: CoroutineScope

    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        coroutineScope = MainScope()
    }

    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        coroutineScope.cancel()
    }

    protected inline fun <T> Flow<T>.observe(crossinline action: suspend (value: T) -> Unit) {
        coroutineScope.launch { collect(action) }
    }
}

/**
 * Return UIComponent that gets re-created using [factory] when [flow] changes.
 */
fun <T> reloadOnChange(
    flow: Flow<T>,
    factory: (T) -> UIComponent?
): UIComponent =
    ReloadingComponent(flow, factory)

/**
 * Return UIComponent that gets re-created using [factory] when either [flow1] or [flow2] changes.
 */
fun <T1, T2> reloadOnChange(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    factory: (T1, T2) -> UIComponent?
): UIComponent =
    ReloadingComponent(combine(flow1, flow2) { v1, v2 -> v1 to v2 }) {
        factory(it.first, it.second)
    }

/**
 * Return UIComponent that gets re-created using [factory] when either [flow1] or [flow2] or
 * [flow3] changes.
 */
fun <T1, T2, T3> reloadOnChange(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    factory: (T1, T2, T3) -> UIComponent?
): UIComponent =
    ReloadingComponent(combine(flow1, flow2, flow3) { v1, v2, v3 -> Triple(v1, v2, v3) }) {
        factory(it.first, it.second, it.third)
    }

/**
 * High Order Component that observes [flow] changes and re-creates child component
 * using [factory] function.
 *
 * usage eg.
 * ```
 *   ReloadingComponent(myFlowValue) { value ->
 *      MyComponent(value)
 *   }
 * ```
 */
class ReloadingComponent<T>(
    private val flow: Flow<T>,
    private val factory: (T) -> UIComponent?
) : UIComponent() {
    private var childComponent: UIComponent? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        flow.observe {
            childComponent?.onDetached(mapboxNavigation)
            childComponent = factory(it)
            childComponent?.onAttached(mapboxNavigation)
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        childComponent?.onDetached(mapboxNavigation)
        childComponent = null
    }
}
