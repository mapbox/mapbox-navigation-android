@file:JvmName("NavigationViewContextFlowable")

package com.mapbox.navigation.dropin.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.ViewBinder
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Helper extension to map [UIBinder] inside a [UICoordinator].
 * Uses a distinct by class to prevent refreshing views of the same type of [UIBinder].
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun <T : UIBinder> NavigationViewContext.flowUiBinder(
    selector: (value: ViewBinder) -> StateFlow<T>,
    mapper: suspend (value: T) -> T = { it }
): Flow<T> {
    return selector(this.uiBinders).map(mapper)
}
