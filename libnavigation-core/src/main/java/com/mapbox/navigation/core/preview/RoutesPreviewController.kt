package com.mapbox.navigation.core.preview

import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.mapToMetadata
import com.mapbox.navigator.RoutesData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@UiThread
internal class RoutesPreviewController(
    private val routesDataParser: RoutesDataParser,
    private val scope: CoroutineScope
) {

    private val mutex = Mutex()

    private var lastUpdate: RoutesPreviewUpdate? = null
        set(value) {
            if (value != field) {
                field = value
                if (value != null) {
                    observers.forEach {
                        it.routesPreviewUpdated(value)
                    }
                }
            }
        }

    private val observers = CopyOnWriteArrayList<RoutesPreviewObserver>()

    fun registerRoutesPreviewObserver(observer: RoutesPreviewObserver) {
        observers.add(observer)
        lastUpdate?.let { observer.routesPreviewUpdated(it) }
    }

    fun unregisterRoutesPreviewObserver(observer: RoutesPreviewObserver) {
        observers.remove(observer)
    }

    fun unregisterAllRoutesPreviewObservers() {
        observers.clear()
    }

    fun getRoutesPreview(): RoutesPreview? = lastUpdate?.routesPreview

    fun previewNavigationRoutes(
        routesToPreview: List<NavigationRoute>,
        primaryRouteIndex: Int = 0,
        onCompleted: () -> Unit = {}
    ) {
        scope.launch {
            mutex.withLock {
                previewRoutesInternal(routesToPreview, primaryRouteIndex)
            }
            onCompleted()
        }
    }

    @Throws(IllegalArgumentException::class)
    fun changeRoutesPreviewPrimaryRoute(route: NavigationRoute) {
        val originalRoutes = lastUpdate?.routesPreview?.originalRoutesList
        require(originalRoutes != null) {
            "no previewed routes are set"
        }
        val routes = originalRoutes.toMutableList()
        require(routes.remove(route)) {
            "route ${route.id} isn't found among the list of previewed routes"
        }
        routes.add(0, route)

        scope.launch {
            mutex.withLock {
                val routesData = routesDataParser.parse(routes)
                val preview = createRoutesPreview(routes, routesData, originalRoutes)
                setNewRoutesPreview(preview)
            }
        }
    }

    private fun setNewRoutesPreview(preview: RoutesPreview) {
        lastUpdate = RoutesPreviewUpdate(
            reason = RoutesPreviewExtra.PREVIEW_NEW,
            routesPreview = preview
        )
    }

    private suspend fun previewRoutesInternal(
        routesToPreview: List<NavigationRoute>,
        primaryRouteIndex: Int
    ) {
        if (routesToPreview.isEmpty()) {
            lastUpdate = RoutesPreviewUpdate(
                RoutesPreviewExtra.PREVIEW_CLEAN_UP,
                null
            )
            return
        }
        val previewedRoutes = movePrimaryRouteToTheBeginning(primaryRouteIndex, routesToPreview)
        val preview =
            createRoutesPreview(
                previewedRoutes,
                routesDataParser.parse(previewedRoutes),
                routesToPreview
            )
        setNewRoutesPreview(preview)
    }

    private fun movePrimaryRouteToTheBeginning(
        primaryRouteIndex: Int,
        routesToPreview: List<NavigationRoute>
    ): List<NavigationRoute> {
        val previewedRoutes = if (primaryRouteIndex == 0) {
            routesToPreview
        } else {
            routesToPreview.toMutableList().apply {
                val primaryRoute = removeAt(primaryRouteIndex)
                add(0, primaryRoute)
            }
        }
        return previewedRoutes
    }

    private fun createRoutesPreview(
        routes: List<NavigationRoute>,
        routesData: RoutesData,
        originalRoutes: List<NavigationRoute>
    ): RoutesPreview {
        val preview = RoutesPreview(
            alternativesMetadata = routesData.alternativeRoutes().map { nativeAlternative ->
                val alternative =
                    originalRoutes.first { it.id == nativeAlternative.route.routeId }
                nativeAlternative.mapToMetadata(alternative)
            },
            routesList = routes,
            originalRoutesList = originalRoutes,
            primaryRouteIndex = originalRoutes.indexOf(routes.first())
        )
        return preview
    }
}

internal fun interface RoutesDataParser {
    suspend fun parse(routes: List<NavigationRoute>): RoutesData
}
