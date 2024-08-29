package com.mapbox.navigation.base.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class RouteOptionsExTest {

    @RunWith(Parameterized::class)
    class DefaultNavOptionsParameterizedTest(
        private val profile: String,
        private val expectedContinueStraight: Boolean,
        private val expectedEnableRefresh: Boolean,
        private val expectedAnnotations: List<String>,
    ) {

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data(): List<out Any> {
                val defaultAnnotations = mutableListOf(
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_DURATION,
                    DirectionsCriteria.ANNOTATION_DISTANCE,
                )

                return listOf(
                    arrayOf(
                        DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                        true,
                        true,
                        defaultAnnotations.plus(
                            listOf(
                                DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                                DirectionsCriteria.ANNOTATION_MAXSPEED,
                                DirectionsCriteria.ANNOTATION_CLOSURE,
                            ),
                        ),
                    ),
                    arrayOf(
                        DirectionsCriteria.PROFILE_DRIVING,
                        true,
                        false,
                        defaultAnnotations.plus(
                            listOf(DirectionsCriteria.ANNOTATION_MAXSPEED),
                        ),
                    ),
                    arrayOf(
                        DirectionsCriteria.PROFILE_CYCLING,
                        false,
                        false,
                        defaultAnnotations,
                    ),
                    arrayOf(
                        DirectionsCriteria.PROFILE_WALKING,
                        false,
                        false,
                        defaultAnnotations,
                    ),
                )
            }
        }

        @Test
        fun testData() {
            val routeOptions = provideRouteOptions(profile).build()

            assertEquals(profile, routeOptions.profile())
            routeOptions.checkDefaultOptions()
            routeOptions.checkAnnotations(expectedAnnotations)
            assertEquals(expectedContinueStraight, routeOptions.continueStraight()!!)
            assertEquals(expectedEnableRefresh, routeOptions.enableRefresh())
        }

        private fun RouteOptions.checkDefaultOptions() {
            assertEquals(DirectionsCriteria.OVERVIEW_FULL, overview())
            assertTrue(steps()!!)
            assertTrue(roundaboutExits()!!)
            assertTrue(voiceInstructions()!!)
            assertTrue(bannerInstructions()!!)
        }

        private fun RouteOptions.checkAnnotations(expectedAnnotations: List<String>) {
            assertTrue(expectedAnnotations.containsAll(annotationsList()!!))
            assertEquals(expectedAnnotations.size, annotationsList()!!.size)
        }

        private fun provideRouteOptions(
            @DirectionsCriteria.ProfileCriteria profile: String,
        ): RouteOptions.Builder =
            RouteOptions.builder()
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(0.0, 0.0),
                        Point.fromLngLat(1.1, 1.1),
                    ),
                )
                .applyDefaultNavigationOptions(profile)
    }
}
