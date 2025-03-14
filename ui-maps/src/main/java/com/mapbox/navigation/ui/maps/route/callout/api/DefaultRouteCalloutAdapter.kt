package com.mapbox.navigation.ui.maps.route.callout.api

import android.content.Context
import android.view.View
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationAnchorConfig
import com.mapbox.maps.viewannotation.annotationAnchors
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.route.callout.model.CalloutViewHolder
import com.mapbox.navigation.ui.maps.route.callout.model.DefaultRouteCalloutAdapterOptions
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutType
import com.mapbox.navigation.ui.maps.route.callout.view.RouteCalloutView

/**
 * Default implementation of [MapboxRouteCalloutAdapter] that introduce two types of callout
 * representation:
 * - [RouteCalloutType.ROUTES_OVERVIEW] - displays ETA for each route
 * - [RouteCalloutType.NAVIGATION] - displays relative difference in ETA between primary and
 * each alternative route, in that case callout for primary route will be hidden
 *
 * @param options defines the appearance of the route callouts, and its behavior
 * @param routeCalloutClickListener notifies when the user clicks on a route callout
 */
@ExperimentalPreviewMapboxNavigationAPI
class DefaultRouteCalloutAdapter(
    private val context: Context,
    options: DefaultRouteCalloutAdapterOptions = DefaultRouteCalloutAdapterOptions.Builder()
        .build(),
    private val routeCalloutClickListener: ((CalloutClickData) -> Unit)? = null,
) : MapboxRouteCalloutAdapter() {

    var options: DefaultRouteCalloutAdapterOptions = options
        private set

    /**
     * Update a subset of route callout options.
     */
    fun updateOptions(options: DefaultRouteCalloutAdapterOptions) {
        this.options = options
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(callout: RouteCallout): CalloutViewHolder {
        val density = context.resources.displayMetrics.density
        val view = RouteCalloutView(context, options, callout)
            .also {
                it.setOnClickListener {
                    routeCalloutClickListener?.invoke(CalloutClickData(callout.route))
                }
            }

        return CalloutViewHolder.Builder(view)
            .options(
                viewAnnotationOptions {
                    ignoreCameraPadding(true)
                    annotationAnchors(
                        {
                            anchor(ViewAnnotationAnchor.TOP_RIGHT)
                            offsetX(BASE_X_OFFSET * density)
                            offsetY(BASE_Y_OFFSET * density)
                        },
                        {
                            anchor(ViewAnnotationAnchor.TOP_LEFT)
                            offsetX(-1.0 * BASE_X_OFFSET * density)
                            offsetY(BASE_Y_OFFSET * density)
                        },
                        {
                            anchor(ViewAnnotationAnchor.BOTTOM_RIGHT)
                            offsetX(BASE_X_OFFSET * density)
                            offsetY(-1.0 * BASE_Y_OFFSET * density)
                        },
                        {
                            anchor(ViewAnnotationAnchor.BOTTOM_LEFT)
                            offsetX(-1.0 * BASE_X_OFFSET * density)
                            offsetY(-1.0 * BASE_Y_OFFSET * density)
                        },
                    )
                },
            ).build()
    }

    override fun onUpdateAnchor(view: View, anchor: ViewAnnotationAnchorConfig) {
        (view as? RouteCalloutView)?.updateAnchor(anchor)
    }

    @ExperimentalPreviewMapboxNavigationAPI
    class CalloutClickData internal constructor(val route: NavigationRoute) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CalloutClickData

            if (route != other.route) return false

            return true
        }

        override fun hashCode(): Int {
            return route.hashCode()
        }

        override fun toString(): String {
            return "CalloutClickData(route=$route)"
        }
    }

    private companion object {
        private const val BASE_X_OFFSET = 7.0
        private const val BASE_Y_OFFSET = 10.0
    }
}
