@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.route.parsing.models.JavaRouteModelsParser
import com.mapbox.navigation.base.internal.route.parsing.models.NRORouteModelsParser
import com.mapbox.navigation.base.internal.utils.PrepareForParsingAction
import com.mapbox.navigation.base.internal.utils.createImmediateNoOptimizationsParsingQueue
import com.mapbox.navigation.base.internal.utils.createOptimizedRoutesParsingQueue
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import kotlinx.coroutines.CoroutineDispatcher

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ParsingEntryPoint(
    private val navigationRoutesParser: NavigationRoutesParser,
    private val routeInterfacesParser: RouteInterfacesParser,
) : NavigationRoutesParser by navigationRoutesParser,
    RouteInterfacesParser by routeInterfacesParser

/**
 * Sets up parsing for production and test usage.
 * In production, parsing is supposed to be setup once as
 * parsers may be connected internally via shared parsing queue.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun setupParsing(
    nativeRoute: Boolean,
    time: Time = Time.SystemClockImpl,
    routeParsingTracking: RouteParsingTracking = noTracking(),
    parsingDispatcher: CoroutineDispatcher = ThreadController.DefaultDispatcher,
    existingParsedRoutesLookup: (id: String) -> NavigationRoute? = { null },
    nnParser: SDKRouteParser = SDKRouteParser.default,
    prepareForParsingAction: PrepareForParsingAction = {},
    loggerFrontend: LoggerFrontend = LoggerProvider.getLoggerFrontend(),
): ParsingEntryPoint {
    val modelParser = if (nativeRoute) {
        NRORouteModelsParser(loggerFrontend)
    } else {
        JavaRouteModelsParser(loggerFrontend)
    }
    val parsingQueue = if (nativeRoute) {
        createImmediateNoOptimizationsParsingQueue()
    } else {
        createOptimizedRoutesParsingQueue(prepareForParsingAction)
    }
    val navigationRoutesParser = NnAndModelsParallelNavigationRoutesParser(
        routeParsingTracking,
        parsingDispatcher,
        time,
        modelParser,
        nnParser,
        parsingQueue,
        loggerFrontend,
    )

    val routeInterfacesParser = JsonResponseOptimizedRouteInterfaceParser(
        existingParsedRoutesLookup,
        parsingDispatcher,
        time,
        modelParser,
        parsingQueue,
    )

    return ParsingEntryPoint(
        navigationRoutesParser,
        routeInterfacesParser,
    )
}
