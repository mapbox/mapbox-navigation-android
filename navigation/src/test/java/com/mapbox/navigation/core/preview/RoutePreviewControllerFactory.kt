package com.mapbox.navigation.core.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope

@ExperimentalPreviewMapboxNavigationAPI
internal fun createRoutePreviewController(
    parentScope: CoroutineScope = TestCoroutineScope(SupervisorJob() + TestCoroutineDispatcher()),
    routesDataParser: RoutesDataParser = RouteDataParserStub(),
): RoutesPreviewController {
    return RoutesPreviewController(
        routesDataParser = routesDataParser,
        scope = parentScope,
    )
}
