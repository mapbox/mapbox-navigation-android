package com.mapbox.navigation.dropin.lifecycle

import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Implement your version of a behavior. Behaviors do not reference android ui elements directly,
 * this is because their lifecycle will survive beyond a view or activity.
 *
 * Behaviors have a lifecycle, contain state, and process actions. [UIComponent] will respond
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
abstract class UIViewModel : MapboxNavigationObserver {

    lateinit var mainJobControl: JobControl

    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mainJobControl = InternalJobControlFactory.createMainScopeJobControl()
    }

    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mainJobControl.job.cancelChildren()
    }
}
