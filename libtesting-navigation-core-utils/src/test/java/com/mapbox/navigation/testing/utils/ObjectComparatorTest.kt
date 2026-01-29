package com.mapbox.navigation.testing.utils

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.testing.factories.createWaypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectComparatorTest {

    @Test
    fun `findDiff returns empty list for same objects`() {
        val obj = TestData("test", 42)
        val result = findDiff(TestData::class.java, obj, obj)

        assertTrue(result.differences.isEmpty())
    }

    @Test
    fun `findDiff returns difference when one field is different`() {
        val expected = TestData("expected")
        val actual = TestData("actual")
        val visitedPaths = mutableSetOf<String>()
        val result = findDiff(
            TestData::class.java,
            expected,
            actual,
            visitedPathCallback = visitedPaths::add,
        )

        assertEquals(1, result.differences.size)
        assertEquals("name", result.differences[0].path)
        assertEquals("expected", result.differences[0].expectedValueString)
        assertEquals("actual", result.differences[0].actualValueString)
    }

    @Test
    fun `difference in child objects`() {
        val expected = TestData(child1 = TestData(value = 1), child2 = TestData(name = "expected"))
        val actual = TestData(child1 = TestData(value = 2), child2 = TestData(name = "actual"))

        val visitedPaths = mutableSetOf<String>()
        val result = findDiff(
            TestData::class.java,
            expected,
            actual,
            visitedPathCallback = visitedPaths::add,
        )
        val differences = result.differences.sortedBy { it.path } // test doesn't verify order

        assertEquals(2, differences.size)
        assertEquals("child1.value", differences[0].path)
        assertEquals("1", differences[0].expectedValueString)
        assertEquals("2", differences[0].actualValueString)

        assertEquals("child2.name", differences[1].path)
        assertEquals("expected", differences[1].expectedValueString)
        assertEquals("actual", differences[1].actualValueString)

        // Verify visited paths - all fields at each level should be visited
        val expectedVisitedPaths = setOf(
            // Root level
            "name",
            "value",
            "child1",
            "child2",
            // child1 level
            "child1.name",
            "child1.value",
            "child1.child1",
            "child1.child2",
            // child2 level
            "child2.name",
            "child2.value",
            "child2.child1",
            "child2.child2",
        )
        assertEquals(expectedVisitedPaths, visitedPaths)
    }

    @Test
    fun `deep difference`() {
        val expected = TestData(child1 = TestData(child2 = TestData(child1 = TestData(value = 9))))
        val actual = TestData(child1 = TestData(child2 = TestData(child1 = TestData(value = 4))))

        val visitedPaths = mutableSetOf<String>()
        val result = findDiff(
            TestData::class.java,
            expected,
            actual,
            visitedPathCallback = visitedPaths::add,
        )

        assertEquals(1, result.differences.size)
        assertEquals("child1.child2.child1.value", result.differences[0].path)
        assertEquals("9", result.differences[0].expectedValueString)
        assertEquals("4", result.differences[0].actualValueString)

        // Verify visited paths - all fields at each level should be visited
        val expectedVisitedPaths = setOf(
            // Root level
            "name",
            "value",
            "child1",
            "child2",
            // child1 level
            "child1.name",
            "child1.value",
            "child1.child1",
            "child1.child2",
            // child1.child2 level
            "child1.child2.name",
            "child1.child2.value",
            "child1.child2.child1",
            "child1.child2.child2",
            // child1.child2.child1 level
            "child1.child2.child1.name",
            "child1.child2.child1.value",
            "child1.child2.child1.child1",
            "child1.child2.child1.child2",
        )
        assertEquals(expectedVisitedPaths, visitedPaths)
    }

    @Test
    fun `different implementations `() {
        val expected = Impl1(value = "expected")
        val actual = Impl2(value = "actual")

        val result = findDiff(BaseData::class.java, expected, actual)

        assertEquals(1, result.differences.size)
        assertEquals("value", result.differences[0].path)
        assertEquals("expected", result.differences[0].expectedValueString)
        assertEquals("actual", result.differences[0].actualValueString)
    }

    @Test
    fun `different implementations - difference in parent of parent`() {
        val expected = Impl1(baseBase = "expected")
        val actual = Impl2(baseBase = "actual")

        val result = findDiff(BaseData::class.java, expected, actual)

        assertEquals(1, result.differences.size)
        assertEquals("baseBaseData", result.differences[0].path)
        assertEquals("expected", result.differences[0].expectedValueString)
        assertEquals("actual", result.differences[0].actualValueString)
    }

    @Test
    fun `different implementations in lists`() {
        class Root(val items: List<BaseData>)

        val expected = Root(listOf(Impl1(), Impl1(value = "expected")))
        val actual = Root(listOf(Impl1(), Impl2(value = "actual")))

        val visitedPaths = mutableSetOf<String>()
        val result = findDiff(
            Root::class.java,
            expected,
            actual,
            visitedPathCallback = visitedPaths::add,
        )

        assertEquals(
            listOf("items[1].value" to "actual"),
            result.differences.map { it.path to it.actualValueString },
        )
        assertEquals(
            "expected",
            result.differences[0].expectedValueString,
        )

        val expectedVisitedPaths = setOf(
            "items",
            "items[0]",
            "items[0].name",
            "items[0].value",
            "items[0].baseBase",
            "items[0].baseBaseData",
            "items[1]",
            //"items[1].name", -- name comparison skipped because it isn't present in common base class
            "items[1].value",
            // "items[1].baseBase", -- the same as above
            "items[1].baseBaseData",
        )
        assertEquals(expectedVisitedPaths, visitedPaths)
    }

    @Test
    fun `some getters aren't implemented`() {
        abstract class A {
            abstract val value1: Int
            abstract val value2: Int
        }

        data class B(override val value1: Int, override val value2: Int) : A()

        val testException = NotImplementedError("test")

        class C(override val value1: Int) : A() {
            override val value2: Int
                get() = throw testException
        }

        val expected = B(1, 2)
        val actual = C(1)

        val result = findDiff(A::class.java, expected, actual)

        assertEquals(1, result.differences.size)
        assertEquals("value2", result.differences[0].path)
        assertEquals("2", result.differences[0].expectedValueString)
        assertEquals(testException.toString(), result.differences[0].actualValueString)
    }

    @Test
    fun `ignore not implemented getters`() {
        abstract class A {
            abstract val value1: Int
            abstract val value2: Int
        }

        data class B(override val value1: Int, override val value2: Int) : A()

        val testException = NotImplementedError("test")

        class C(override val value1: Int) : A() {
            override val value2: Int
                get() = throw testException
        }

        val expected = B(1, 2)
        val actual = C(1)

        val result = findDiff(
            A::class.java,
            expected,
            actual,
            ignoreGetters = listOf("value2"),
        )

        assertEquals(0, result.differences.size)
    }

    @Test
    fun compareEqualDirectionsResponses() {
        val testDirectionsResponse = createDirectionsResponse()
        val copy = testDirectionsResponse.toBuilder().build()

        assertNoDiffs(testDirectionsResponse, copy)
    }

    @Test
    fun compareDirectionsResponsesWithDifferentWaypoints() {
        val testDirectionsResponse = createDirectionsResponse()
        val copyWithoutWaypoints = testDirectionsResponse
            .toBuilder()
            .waypoints(null)
            .build()

        val result = findDiff(
            DirectionsResponse::class.java,
            testDirectionsResponse,
            copyWithoutWaypoints,
        )

        assertEquals(1, result.differences.size)
        assertEquals("waypoints", result.differences[0].path)
    }

    @Test
    fun compareListsWithDifferentElement() {
        data class Item(val value: Int)
        data class Container(val items: List<Item>)

        val expected = Container(listOf(Item(1), Item(2), Item(3)))
        val actual = Container(listOf(Item(1), Item(999), Item(3)))

        val visitedPaths = mutableSetOf<String>()
        val result = findDiff(
            Container::class.java,
            expected,
            actual,
            visitedPathCallback = visitedPaths::add,
        )

        assertTrue("Should find at least one difference", result.differences.isNotEmpty())
        val paths = result.differences.map { it.path }.joinToString()
        assertTrue("Path should contain items[1]: $paths", paths.contains("items[1]"))

        // Verify visited paths - all list elements should be visited, including those with differences
        val expectedVisitedPaths = setOf(
            "items",
            "items[0]",
            "items[0].value",
            "items[1]",
            "items[1].value",
            "items[2]",
            "items[2].value",
        )
        assertEquals(expectedVisitedPaths, visitedPaths)
    }

    @Test
    fun compareWaypoints() {
        val waypoint1 = createWaypoint(distance = 100.0)
        val waypoint2 = createWaypoint(distance = 999.0)

        val list1 = listOf(waypoint1, waypoint1)
        val list2 = listOf(waypoint1, waypoint2)

        // Test direct list comparison
        val result = findDiff(List::class.java, list1, list2)

        assertTrue("Should find differences", result.differences.isNotEmpty())
    }

    @Test
    fun compareDirectionsResponsesWithDifferenceInsideSecondWaypoint() {
        val testDirectionsResponse = createDirectionsResponse()
        val secondWaypointDistance = 999.0
        val copyWithoutWaypoints = testDirectionsResponse
            .toBuilder()
            .waypoints(
                testDirectionsResponse.waypoints()?.mapIndexed { index, waypoint ->
                    if (index == 1) {
                        waypoint.toBuilder().distance(secondWaypointDistance).build()
                    } else {
                        waypoint
                    }
                },
            )
            .build()

        val result = findDiff(
            DirectionsResponse::class.java,
            testDirectionsResponse,
            copyWithoutWaypoints,
        )

        assertEquals(1, result.differences.size)
        assertEquals("waypoints[1].distance", result.differences[0].path)
        assertEquals(
            testDirectionsResponse.waypoints()!![1].distance().toString(),
            result.differences[0].expectedValueString,
        )
        assertEquals(secondWaypointDistance.toString(), result.differences[0].actualValueString)
    }

    @Test
    fun compareDirectionsResponsesWithDifferentRouteWeightAndIncidentType() {
        val incident1 = createIncident(id = "1", type = Incident.INCIDENT_CONSTRUCTION)
        val incident2 = createIncident(id = "2", type = Incident.INCIDENT_ACCIDENT)
        val annotation = createRouteLegAnnotation(duration = listOf(2.0, 3.0, 4.0))
        val routeLeg = createRouteLeg(
            incidents = listOf(incident1, incident2),
            annotation = annotation,
        )
        val testDirectionsResponse = createDirectionsResponse(
            routes = listOf(
                createDirectionsRoute(
                    legs = listOf(routeLeg),
                    distance = 5.0,
                    duration = 9.0,
                ).toBuilder()
                    .weight(100.0)
                    .build(),
            ),
        )

        val modifiedResponse = testDirectionsResponse.let { response ->
            response.toBuilder()
                .routes(
                    response.routes().mapIndexed { routeIndex, route ->
                        if (routeIndex == 0) {
                            route.toBuilder()
                                .weight(200.0) // Different weight
                                .legs(
                                    route.legs()?.mapIndexed { legIndex, leg ->
                                        if (legIndex == 0) {
                                            leg.toBuilder()
                                                .incidents(
                                                    leg.incidents()
                                                        ?.mapIndexed { incidentIndex, incident ->
                                                            if (incidentIndex == 1) {
                                                                // Change type of second incident
                                                                incident.toBuilder()
                                                                    .type(Incident.INCIDENT_WEATHER)
                                                                    .build()
                                                            } else {
                                                                incident
                                                            }
                                                        },
                                                )
                                                .annotation(
                                                    leg.annotation()?.let { ann ->
                                                        // Change one element in the middle of duration array
                                                        ann.toBuilder()
                                                            .duration(
                                                                ann.duration()
                                                                    ?.mapIndexed { index, value ->
                                                                        if (index == 1) {
                                                                            999.0 // Different duration in the middle
                                                                        } else {
                                                                            value
                                                                        }
                                                                    },
                                                            )
                                                            .build()
                                                    },
                                                )
                                                .build()
                                        } else {
                                            leg
                                        }
                                    },
                                )
                                .build()
                        } else {
                            route
                        }
                    },
                )
                .build()
        }

        val result = findDiff(
            DirectionsResponse::class.java,
            testDirectionsResponse,
            modifiedResponse,
        )

        assertEquals(3, result.differences.size)
        val paths = result.differences.map { it.path }.sorted()
        assertEquals(
            listOf(
                "routes[0].legs[0].annotation.duration[1]",
                "routes[0].legs[0].incidents[1].type",
                "routes[0].weight",
            ),
            paths,
        )
    }


    @Test
    fun `option to ignore null and empty arrays - disabled`() {
        class Test(val items: List<String>?)

        val result = findDiff(
            Test::class.java,
            Test(emptyList()),
            Test(null),
            nullAndEmptyArraysAreTheSame = false,
        )

        assertEquals(
            listOf("items"),
            result.differences.map { it.path },
        )
    }

    @Test
    fun `option to ignore null and empty arrays - enabled`() {
        class Test(val items: List<String>?)

        val result = findDiff<Test>(
            Test::class.java,
            Test(emptyList()),
            Test(null),
            nullAndEmptyArraysAreTheSame = true,
        )

        assertEquals(emptyList<Difference>(), result.differences)
    }

    @Test
    fun `option to ignore null and empty strings - enabled`() {
        class Test(val items: List<String?>)

        val result = findDiff(
            Test::class.java,
            Test(listOf("", null, "")),
            Test(listOf(null, "", "")),
            nullAndEmptyStringsAreTheSame = true,
        )

        assertEquals(
            emptyList<String>(),
            result.differences.map { it.path },
        )
    }

    @Test
    fun `option to ignore null and empty strings - disabled`() {
        class Test(val items: List<String?>)

        val result = findDiff(
            Test::class.java,
            Test(listOf("", null, "")),
            Test(listOf(null, "", "")),
            nullAndEmptyStringsAreTheSame = false,
        )

        assertEquals(
            listOf("items[0]", "items[1]"),
            result.differences.map { it.path },
        )
    }

    @Test
    fun `doubles comparison`() {
        class Test(
            val double1: Double,
            val double2: Double,
            val double3: Double,
        )

        val result = findDiff(
            Test::class.java,
            Test(
                double1 = 5.000001,
                double2 = -1.999999,
                double3 = Double.NaN,
            ),
            Test(
                double1 = 5.000002,
                double2 = -1.98,
                double3 = Double.NaN,
            ),
            nullAndEmptyArraysAreTheSame = false,
            doubleComparisonEpsilon = 0.01,
        )

        assertEquals(
            listOf("double2"),
            result.differences.map { it.path },
        )
    }

    @Test
    fun `compare directions routes with difference in charging waypoint metadata`() {
        val route1 = createDirectionsRoute(
            waypoints = listOf(
                createWaypoint(),
                createWaypoint(
                    unrecognizedProperties = mapOf(
                        "charging_metadata" to JsonObject().apply {
                            add("name", JsonPrimitive("test_charging"))
                            add("restrictions", JsonObject())
                            add("testArray", JsonArray())
                        },
                    ),
                ),
            )
        )
        val route2 = createDirectionsRoute(
            waypoints = listOf(
                createWaypoint(),
                createWaypoint(
                    unrecognizedProperties = mapOf(
                        "charging_metadata" to JsonObject().apply {
                            add("name", JsonPrimitive("test_charging"))
                            add("restrictions", JsonNull.INSTANCE)
                            add("testArray", JsonNull.INSTANCE)
                        },
                    ),
                ),
            )
        )


        val result = findDiff(
            DirectionsRoute::class.java,
            route1,
            route2,
        )

        val paths = result.differences.map { it.path }.sorted()
        assertEquals(
            listOf(
                "waypoints[1].unrecognizedJsonProperties.charging_metadata.restrictions",
                "waypoints[1].unrecognizedJsonProperties.charging_metadata.testArray",
            ),
            paths,
        )
    }

    @Test
    fun `compare directions routes with difference in charging waypoint null-empty arrays`() {
        val route1 = createDirectionsRoute(
            waypoints = listOf(
                createWaypoint(),
                createWaypoint(
                    unrecognizedProperties = mapOf(
                        "charging_metadata" to JsonObject().apply {
                            add("testArray", JsonArray())
                        },
                    ),
                ),
            )
        )
        val route2 = createDirectionsRoute(
            waypoints = listOf(
                createWaypoint(),
                createWaypoint(
                    unrecognizedProperties = mapOf(
                        "charging_metadata" to JsonObject().apply {
                            add("testArray", JsonNull.INSTANCE)
                        },
                    ),
                ),
            )
        )

        val result = findDiff(
            DirectionsRoute::class.java,
            route1,
            route2,
            nullAndEmptyArraysAreTheSame = true,
        )

        val paths = result.differences.map { it.path }.sorted()
        assertEquals(
            emptyList<String>(),
            paths,
        )
    }

    @Test
    fun `null and empty json objects are equals`() {
        val route1 = createDirectionsRoute(
            waypoints = listOf(
                createWaypoint(),
                createWaypoint(
                    unrecognizedProperties = mapOf(
                        "charging_metadata" to JsonObject().apply {
                            add("restrictions", JsonObject())
                        },
                    ),
                ),
            )
        )
        val route2 = createDirectionsRoute(
            waypoints = listOf(
                createWaypoint(),
                createWaypoint(
                    unrecognizedProperties = mapOf(
                        "charging_metadata" to JsonObject().apply {
                            add("restrictions", JsonNull.INSTANCE)
                        },
                    ),
                ),
            )
        )


        val result = findDiff(
            DirectionsRoute::class.java,
            route1,
            route2,
            nullAndEmptyJsonObjectsAreTheSame = true,
        )

        val paths = result.differences.map { it.path }.sorted()
        assertEquals(
            emptyList<String>(),
            paths,
        )
    }

    private class TestData(
        val name: String = "test",
        val value: Int = 3,
        val child1: TestData? = null,
        val child2: TestData? = null,
    )

    private abstract class BaseOfBaseData(
        open val baseBaseData: String,
    )

    private abstract class BaseData(baseBase: String) : BaseOfBaseData(baseBase) {
        abstract val value: String
    }

    private data class Impl1(
        val name: String = "impl1",
        override val value: String = "test",
        val baseBase: String = "basebase",
    ) : BaseData(baseBase)

    private class Impl2(
        val name: String = "impl2",
        override val value: String = "test",
        baseBase: String = "basebase",
    ) : BaseData(baseBase)
}

