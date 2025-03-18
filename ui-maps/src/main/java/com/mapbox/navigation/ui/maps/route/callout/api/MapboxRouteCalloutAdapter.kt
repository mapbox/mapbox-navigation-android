package com.mapbox.navigation.ui.maps.route.callout.api

import android.view.View
import com.mapbox.maps.ViewAnnotationAnchorConfig
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.callout.model.CalloutViewHolder
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Adapters provide a binding from an app-specific data set to callout views that are attached to
 * each route line.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class MapboxRouteCalloutAdapter {

    private val observers: CopyOnWriteArraySet<() -> Unit> = CopyOnWriteArraySet()

    /**
     * Called when [MapboxRouteLineView] needs a new [CalloutViewHolder] representing [RouteCallout]
     */
    abstract fun onCreateViewHolder(callout: RouteCallout): CalloutViewHolder

    /**
     * Called when view callout anchor has changed.
     * When it triggers it means that view callout placement relative to its anchor coordinate has
     * been updated.
     */
    abstract fun onUpdateAnchor(view: View, anchor: ViewAnnotationAnchorConfig)

    /**
     * Notifies the adapter that the app-specific data set has changed.
     */
    fun notifyDataSetChanged() {
        observers.forEach { it.invoke() }
    }

    internal fun registerDataObserver(observer: () -> Unit) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    internal fun removeDataObserver(observer: () -> Unit) {
        observers.remove(observer)
    }
}
