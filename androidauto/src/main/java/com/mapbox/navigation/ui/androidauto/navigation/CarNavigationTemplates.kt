package com.mapbox.navigation.ui.androidauto.navigation

import android.text.SpannableString
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.DurationSpan
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.NavigationTemplate
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.preview.RoutesPreview

/**
 * Builds the templates of the Mapbox navigation screen. The builders have no state and no side
 * effects, so apps with their own screens can use them to show the same templates.
 */
@ExperimentalPreviewMapboxNavigationAPI
object CarNavigationTemplates {

    /**
     * Builds the free drive [NavigationTemplate] used by the Mapbox navigation screen.
     *
     * @param actionStrip the action strip of the template.
     * @param mapActionStrip the map action strip, e.g. from
     * [com.mapbox.navigation.ui.androidauto.action.MapboxMapActionStrip].
     */
    fun freeDrive(
        actionStrip: ActionStrip,
        mapActionStrip: ActionStrip,
    ): NavigationTemplate = NavigationTemplate.Builder()
        .setBackgroundColor(CarColor.PRIMARY)
        .setActionStrip(actionStrip)
        .setMapActionStrip(mapActionStrip)
        .build()

    /**
     * Builds the active guidance [NavigationTemplate] used by the Mapbox navigation screen.
     *
     * @param navigationInfo maneuver and travel estimate, e.g. from [CarNavigationInfoProvider].
     * @param actionStrip the action strip of the template.
     * @param mapActionStrip the map action strip, e.g. from
     * [com.mapbox.navigation.ui.androidauto.action.MapboxMapActionStrip].
     */
    fun activeGuidance(
        navigationInfo: CarNavigationInfo,
        actionStrip: ActionStrip,
        mapActionStrip: ActionStrip,
    ): NavigationTemplate = NavigationTemplate.Builder()
        .setBackgroundColor(CarColor.PRIMARY)
        .setActionStrip(actionStrip)
        .setMapActionStrip(mapActionStrip)
        .apply {
            navigationInfo.navigationInfo?.let(::setNavigationInfo)
            navigationInfo.destinationTravelEstimate?.let(::setDestinationTravelEstimate)
        }
        .build()

    /**
     * Builds the route preview list used as the content of a
     * [androidx.car.app.navigation.model.MapWithContentTemplate] by the Mapbox navigation screen.
     * Every row shows the duration, summary and distance of a route and has a navigate action.
     * An empty [RoutesPreview.originalRoutesList] shows a loading list.
     *
     * @param routesPreview the routes to choose from. Rows follow
     * [RoutesPreview.originalRoutesList] and the row at [RoutesPreview.primaryRouteIndex] is
     * selected.
     * @param title the title of the list header.
     * @param navigateActionTitle the title of the navigate action.
     * @param formatDistance formats a route distance in meters.
     * @param onRouteSelected invoked with the
     * [com.mapbox.navigation.base.route.NavigationRoute.id] of the row the driver selected.
     * @param onNavigate invoked with the [com.mapbox.navigation.base.route.NavigationRoute.id] of
     * the row whose navigate action the driver tapped.
     * @param navigateActionIcon optional icon of the navigate action.
     */
    fun routePreview(
        routesPreview: RoutesPreview,
        title: CharSequence,
        navigateActionTitle: CharSequence,
        formatDistance: (Double) -> CharSequence,
        onRouteSelected: (routeId: String) -> Unit,
        onNavigate: (routeId: String) -> Unit,
        navigateActionIcon: CarIcon? = null,
    ): ListTemplate {
        val templateBuilder = ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(title)
                    .setStartHeaderAction(Action.BACK)
                    .build(),
            )

        if (routesPreview.originalRoutesList.isEmpty()) {
            return templateBuilder
                .setLoading(true)
                .build()
        }

        val listBuilder = ItemList.Builder()

        val routes = routesPreview.originalRoutesList
        routes.forEach { navigationRoute ->
            // Each row starts its own route, not the currently selected one. Routes are passed by id
            // so a click from an outdated template can't start a route from a newer preview.
            val navigateAction = Action.Builder()
                .setTitle(navigateActionTitle)
                .setOnClickListener { onNavigate(navigationRoute.id) }
                .apply {
                    navigateActionIcon?.let(::setIcon)
                }
                .build()
            val route = navigationRoute.directionsRoute
            val routeSummary = route.legs()?.firstOrNull()?.summary().orEmpty()
            val routeTitle = SpannableString("  $routeSummary").apply {
                setSpan(DurationSpan.create(route.duration().toLong()), 0, 1, 0)
            }
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(routeTitle)
                    .addText(formatDistance(route.distance()))
                    .addAction(navigateAction)
                    .build(),
            )
        }

        listBuilder.setSelectedIndex(routesPreview.primaryRouteIndex)
        listBuilder.setOnSelectedListener { index ->
            routes.getOrNull(index)?.let { onRouteSelected(it.id) }
        }

        return templateBuilder
            .setSingleList(listBuilder.build())
            .build()
    }
}
