package com.mapbox.androidauto.car.preview

import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import com.mapbox.androidauto.ActiveGuidanceState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponentContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Used with the [CarRouteLine]. This allows you to preview a route before assigning it to
 * [MapboxNavigation] for turn by turn audio guidance.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CarRoutePreviewContract(
    private val originalNavigationRoutes: List<NavigationRoute>
) : RouteLineComponentContract {

    private val mutableRoutes = MutableStateFlow(originalNavigationRoutes)

    /**
     * Use [selectIndex] to update the selected route index.
     */
    var selectedIndex = 0
        private set

    /**
     * Choose a route index. [MapboxNavigation] accepts routes with the primary route
     * at the zero index. Selecting the index will update the route line, but will not re-order
     * the list items shown by the [ItemList.Builder]. Call [Screen.invalidate] when the selection
     * has changed.
     *
     * @return true when the value has changed, false otherwise.
     */
    fun selectIndex(index: Int): Boolean {
        if (selectedIndex == index) return false
        selectedIndex = index
        val newRouteOrder = mutableRoutes.value.toMutableList()
        if (index > 0) {
            val swap = newRouteOrder[0]
            newRouteOrder[0] = newRouteOrder[index]
            newRouteOrder[index] = swap
            mutableRoutes.value = newRouteOrder
        } else {
            mutableRoutes.value = originalNavigationRoutes
        }
        return true
    }

    /**
     * Call when ready to start active guidance with the current selection.
     */
    fun startActiveNavigation() {
        MapboxNavigationApp.current()?.setNavigationRoutes(mutableRoutes.value)
        MapboxCarApp.updateCarAppState(ActiveGuidanceState)
    }

    override fun getRouteInPreview(): Flow<List<NavigationRoute>?> {
        return mutableRoutes
    }

    override fun setRoutes(mapboxNavigation: MapboxNavigation, routes: List<NavigationRoute>) {
        mutableRoutes.value = routes
    }
}
