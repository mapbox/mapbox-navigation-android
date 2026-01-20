package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.deserializeNavigationRouteFrom
import com.mapbox.navigation.base.internal.route.serialize
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.assumeNotNROBecauseOfSerialization
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class NavigationRouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @Test
    fun native_route_parser_uses_response_uuid_and_index() = sdkTest {
        withMapboxNavigation { navigation ->
            val routes = navigation.requestMockRoutes(
                mockWebServerRule,
                RoutesProvider.dc_short_with_alternative(activity),
            )
            assertEquals("${routes[0].responseUUID}#0", routes[0].id)
            assertEquals("${routes[1].responseUUID}#1", routes[1].id)
        }
    }

    @Test
    fun native_route_parser_backfills_unique_identifier_when_uuid_and_index_missing() = sdkTest {
        withMapboxNavigation { navigation ->
            val routes = navigation.requestMockRoutes(
                mockWebServerRule,
                RoutesProvider.dc_short_with_alternative_no_uuid(activity),
            )
            // assert that Nav Native backfilled a local unique identifier
            assertTrue(routes[0].id.matches(Regex("^local@(.+)#0")))
            assertTrue(routes[1].id.matches(Regex("^local@(.+)#1")))
            assertNotEquals(routes[0].id, routes[1].id)
        }
    }

    @Test
    fun deserialize_serialize_routes() = sdkTest {
        assumeNotNROBecauseOfSerialization()
        withMapboxNavigation { navigation ->
            val mockRoute = RoutesProvider.route_alternative_with_closure(context)

            val routes = navigation.requestMockRoutes(
                mockWebServerRule,
                mockRoute,
            )
            routes.forEach { route ->
                val deserializationResult = withContext(Dispatchers.Default) {
                    val serialized = route.serialize()
                    deserializeNavigationRouteFrom(serialized)
                }
                assertNull(
                    "error for ${route.id}",
                    deserializationResult.error,
                )
                val deserializedRoute = deserializationResult.value!!
                assertEquals(
                    route,
                    deserializedRoute,
                )
                assertEquals(
                    route.origin,
                    deserializedRoute.origin,
                )
                // They aren't equal because of different object locations instances
                // which are compared by ref equality.
                // Should be fixed by NAVAND-1719
                // assertEquals(
                //    route.upcomingRoadObjects,
                //    deserializedRoute.upcomingRoadObjects
                // )
            }
        }
    }
}
