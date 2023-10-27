package com.mapbox.navigation.core.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RoutesData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesPreviewControllerTest {

    @Test
    fun `default state`() {
        val routesPreviewController = createRoutePreviewController()

        var previewUpdate: RoutesPreviewUpdate? = null
        routesPreviewController.registerRoutesPreviewObserver {
            previewUpdate = it
        }
        val preview = routesPreviewController.getRoutesPreview()

        assertNull(previewUpdate)
        assertNull(preview)
    }

    @Test
    fun `set previewed routes`() {
        val routesPreviewController = createRoutePreviewController()
        var previewUpdate: RoutesPreviewUpdate? = null
        routesPreviewController.registerRoutesPreviewObserver {
            previewUpdate = it
        }
        var completed = false

        val testRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute()
                )
            )
        )
        routesPreviewController.previewNavigationRoutes(testRoutes) {
            completed = true
        }

        assertNotNull(previewUpdate)
        assertEquals(previewUpdate!!.routesPreview, routesPreviewController.getRoutesPreview())
        assertEquals(RoutesPreviewExtra.PREVIEW_NEW, previewUpdate!!.reason)
        assertNotNull(previewUpdate!!.routesPreview)
        val preview = previewUpdate!!.routesPreview!!
        assertEquals(testRoutes, preview.originalRoutesList)
        assertEquals(testRoutes, preview.routesList)
        assertEquals(testRoutes.first(), preview.primaryRoute)
        assertEquals(testRoutes[1], preview.alternativesMetadata.first().navigationRoute)
        assertEquals(0, preview.primaryRouteIndex)
        assertTrue(completed)
    }

    @Test
    fun `register observer when preview is active`() {
        val routesPreviewController = createRoutePreviewController()
        val testRoutes = createNavigationRoutes()
        routesPreviewController.previewNavigationRoutes(testRoutes)

        var previewUpdate: RoutesPreviewUpdate? = null
        routesPreviewController.registerRoutesPreviewObserver {
            previewUpdate = it
        }

        assertNotNull(previewUpdate)
        assertEquals(previewUpdate!!.routesPreview, routesPreviewController.getRoutesPreview())
    }

    @Test
    fun `set previewed routes with the second route as a primary`() {
        val routesPreviewController = createRoutePreviewController()
        var previewUpdate: RoutesPreviewUpdate? = null
        routesPreviewController.registerRoutesPreviewObserver {
            previewUpdate = it
        }

        val testRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute()
                )
            )
        )
        routesPreviewController.previewNavigationRoutes(testRoutes, primaryRouteIndex = 1)

        assertNotNull(previewUpdate)
        assertNotNull(previewUpdate!!.routesPreview)
        val preview = previewUpdate!!.routesPreview!!
        assertEquals(testRoutes, preview.originalRoutesList)
        assertEquals(listOf("test#1", "test#0"), preview.routesList.map { it.id })
        assertEquals(testRoutes[1], preview.primaryRoute)
        assertEquals(testRoutes[0], preview.alternativesMetadata.first().navigationRoute)
        assertEquals(1, preview.primaryRouteIndex)
    }

    @Test
    fun `preview the same set of routes a few times`() {
        val routesPreviewController = createRoutePreviewController()
        var eventCount = 0
        routesPreviewController.registerRoutesPreviewObserver {
            eventCount++
        }

        val testRoutes = createNavigationRoutes()
        routesPreviewController.previewNavigationRoutes(testRoutes)
        routesPreviewController.previewNavigationRoutes(testRoutes)
        routesPreviewController.previewNavigationRoutes(testRoutes)

        assertEquals(1, eventCount)
    }

    @Test
    fun `cleanup routes a few times`() {
        val routesPreviewController = createRoutePreviewController()
        var eventCount = 0
        routesPreviewController.registerRoutesPreviewObserver {
            eventCount++
        }

        routesPreviewController.previewNavigationRoutes(createNavigationRoutes())
        routesPreviewController.previewNavigationRoutes(emptyList())
        routesPreviewController.previewNavigationRoutes(emptyList())
        routesPreviewController.previewNavigationRoutes(emptyList())

        assertEquals(
            2, // one for set and one for cleanup
            eventCount
        )
    }

    @Test
    fun `select different primary route`() {
        val routesPreviewController = createRoutePreviewController()
        var previewUpdate: RoutesPreviewUpdate? = null
        routesPreviewController.registerRoutesPreviewObserver {
            previewUpdate = it
        }
        val testRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute()
                )
            )
        )
        routesPreviewController.previewNavigationRoutes(testRoutes)

        routesPreviewController.changeRoutesPreviewPrimaryRoute(
            previewUpdate!!.routesPreview!!.alternativesMetadata.first().navigationRoute
        )

        val result = previewUpdate!!.routesPreview
        assertNotNull(result)
        result!!
        assertEquals(testRoutes[1], result.primaryRoute)
        assertEquals(testRoutes[0], result.alternativesMetadata.first().navigationRoute)
        assertEquals(testRoutes, result.originalRoutesList)
        assertEquals(
            listOf(
                testRoutes[1],
                testRoutes[0]
            ),
            result.routesList
        )
        assertEquals(1, result.primaryRouteIndex)
    }

    @Test
    fun `switch between previewed routes quicker then internal processing`() {
        val testRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                )
            )
        )
        val waitHandle = CompletableDeferred<Unit>()
        val routesPreviewController = createRoutePreviewController(
            routesDataParser = {
                if (it.first().id == "test#1") {
                    waitHandle.await()
                }
                RouteDataParserStub().parse(it)
            }
        )
        val previewedRoutesIds = mutableListOf<List<String>?>()
        routesPreviewController.registerRoutesPreviewObserver {
            previewedRoutesIds.add(it.routesPreview?.routesList?.map { it.id })
        }
        routesPreviewController.previewNavigationRoutes(testRoutes)

        routesPreviewController.changeRoutesPreviewPrimaryRoute(testRoutes[1])
        routesPreviewController.changeRoutesPreviewPrimaryRoute(testRoutes[2])
        waitHandle.complete(Unit)

        assertEquals(
            listOf(
                listOf("test#0", "test#1", "test#2"),
                listOf("test#1", "test#0", "test#2"),
                listOf("test#2", "test#0", "test#1")
            ),
            previewedRoutesIds
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `select wrong primary route`() {
        val routesPreviewController = createRoutePreviewController()
        routesPreviewController.previewNavigationRoutes(
            createNavigationRoutes(
                response = createDirectionsResponse(
                    uuid = "test",
                    routes = listOf(
                        createDirectionsRoute(),
                        createDirectionsRoute()
                    )
                )
            )
        )

        routesPreviewController.changeRoutesPreviewPrimaryRoute(
            createNavigationRoutes().first()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `select primary route without previewing any routes`() {
        val routesPreviewController = createRoutePreviewController()

        routesPreviewController.changeRoutesPreviewPrimaryRoute(
            createNavigationRoutes().first()
        )
    }

    @Test
    fun `unsubscribe from preview updates`() {
        val routesPreviewController = createRoutePreviewController()
        var eventsCount = 0
        val observer = RoutesPreviewObserver { eventsCount++ }

        routesPreviewController.registerRoutesPreviewObserver(observer)
        routesPreviewController.unregisterRoutesPreviewObserver(observer)
        routesPreviewController.previewNavigationRoutes(
            createNavigationRoutes(
                createDirectionsResponse(uuid = "test1")
            )
        )
        routesPreviewController.previewNavigationRoutes(
            createNavigationRoutes(
                createDirectionsResponse(uuid = "test2")
            )
        )

        assertEquals(0, eventsCount)
    }

    @Test
    fun `remove all observers`() {
        val routesPreviewController = createRoutePreviewController()
        var eventsCount = 0
        routesPreviewController.registerRoutesPreviewObserver {
            eventsCount++
        }
        routesPreviewController.registerRoutesPreviewObserver {
            eventsCount++
        }

        routesPreviewController.unregisterAllRoutesPreviewObservers()
        routesPreviewController.previewNavigationRoutes(
            createNavigationRoutes(
                createDirectionsResponse(uuid = "test1")
            )
        )
        routesPreviewController.previewNavigationRoutes(
            createNavigationRoutes(
                createDirectionsResponse(uuid = "test2")
            )
        )

        assertEquals(0, eventsCount)
    }

    @Test
    fun `clean-up routes preview`() {
        val routesPreviewController = createRoutePreviewController()
        var previewUpdate: RoutesPreviewUpdate? = null
        routesPreviewController.registerRoutesPreviewObserver {
            previewUpdate = it
        }
        val testRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute()
                )
            )
        )
        routesPreviewController.previewNavigationRoutes(testRoutes)

        routesPreviewController.previewNavigationRoutes(emptyList())

        assertNotNull(previewUpdate)
        val result = previewUpdate!!
        assertEquals(RoutesPreviewExtra.PREVIEW_CLEAN_UP, result.reason)
        assertNull(result.routesPreview)
    }

    @Test
    fun `one of alternatives are invalid`() {
        val testRoutes = listOf(
            createNavigationRoutes(createDirectionsResponse(uuid = "primary")).first(),
            createNavigationRoutes(createDirectionsResponse(uuid = "valid")).first(),
            createNavigationRoutes(createDirectionsResponse(uuid = "invalid")).first(),
        )
        val routesPreviewController = createRoutePreviewController(
            routesDataParser = {
                object : RoutesData {
                    override fun primaryRoute(): RouteInterface = testRoutes.first().nativeRoute()

                    // data for the invalid alternative isn't returned
                    override fun alternativeRoutes(): MutableList<RouteAlternative> {
                        return mutableListOf(
                            mockk(relaxed = true) {
                                every { route } returns testRoutes[1].nativeRoute()
                            }
                        )
                    }
                }
            }
        )

        routesPreviewController.previewNavigationRoutes(testRoutes)
        val routesPreview = routesPreviewController.getRoutesPreview()

        assertNotNull(routesPreview)
        routesPreview!!
        assertEquals(
            listOf("valid#0"),
            routesPreview.alternativesMetadata.map { it.navigationRoute.id }
        )
        assertEquals(testRoutes, routesPreview.routesList)
        assertEquals(testRoutes, routesPreview.originalRoutesList)
    }

    @Test
    fun `new routes are set faster then processing`() {
        val slow = createNavigationRoutes(createDirectionsResponse(uuid = "slow"))
        val slowWaitHandle = CompletableDeferred<Unit>()
        val fast = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "fast",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute()
                )
            )
        )
        val routesPreviewController = createRoutePreviewController(
            routesDataParser = {
                if (it == slow) {
                    slowWaitHandle.await()
                }
                RouteDataParserStub().parse(it)
            }
        )
        val previewedRoutes = mutableListOf<List<String>?>()
        routesPreviewController.registerRoutesPreviewObserver {
            previewedRoutes.add(it.routesPreview?.routesList?.map(NavigationRoute::id))
        }

        routesPreviewController.previewNavigationRoutes(slow)
        routesPreviewController.previewNavigationRoutes(fast)
        routesPreviewController.previewNavigationRoutes(emptyList())
        slowWaitHandle.complete(Unit)

        assertEquals(
            listOf(
                listOf("slow#0"),
                listOf("fast#0", "fast#1"),
                null
            ),
            previewedRoutes
        )
    }

    @Test
    fun `select primary route while new route is processing`() {
        val slow = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "slow",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute()
                )
            )
        )
        val slowWaitHandle = CompletableDeferred<Unit>()
        val fast = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "fast",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute()
                )
            )
        )
        val routesPreviewController = createRoutePreviewController(
            routesDataParser = {
                if (it == slow) {
                    slowWaitHandle.await()
                }
                RouteDataParserStub().parse(it)
            }
        )
        val previewedRoutes = mutableListOf<List<String>?>()
        routesPreviewController.registerRoutesPreviewObserver {
            previewedRoutes.add(it.routesPreview?.routesList?.map(NavigationRoute::id))
        }

        routesPreviewController.previewNavigationRoutes(fast)
        routesPreviewController.previewNavigationRoutes(slow)
        // user clicks on UI with old routes
        routesPreviewController.changeRoutesPreviewPrimaryRoute(fast[1])
        slowWaitHandle.complete(Unit)

        assertEquals(
            listOf(
                listOf("fast#0", "fast#1"),
                listOf("slow#0", "slow#1"),
                listOf("fast#1", "fast#0"),
            ),
            previewedRoutes
        )
    }
}
