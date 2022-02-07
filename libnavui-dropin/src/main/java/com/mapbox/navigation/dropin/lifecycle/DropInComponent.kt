package com.mapbox.navigation.dropin.lifecycle

import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Using the [DropInComponent] gives you access to a [coroutineScope]. All coroutines that you
 * launch inside onAttached will be canceled when the observer is detached. You do not need to
 * implement onDetached for your flowable components.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
open class DropInComponent : MapboxNavigationObserver {

    lateinit var coroutineScope: CoroutineScope

    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        coroutineScope = MainScope()
    }

    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        coroutineScope.cancel()
    }
}
