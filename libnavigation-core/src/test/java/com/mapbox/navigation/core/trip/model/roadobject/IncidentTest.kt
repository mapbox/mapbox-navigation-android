package com.mapbox.navigation.core.trip.model.roadobject

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentCongestion
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentImpact
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentType
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import kotlin.reflect.KClass

class IncidentTest : BuilderTest<Incident, Incident.Builder>() {

    override fun getImplementationClass(): KClass<Incident> = Incident::class

    override fun getFilledUpBuilder(): Incident.Builder =
        Incident.Builder(
            RoadObjectGeometry.Builder(
                456.0,
                LineString.fromLngLats(
                    listOf(Point.fromLngLat(10.0, 20.0), Point.fromLngLat(33.0, 44.0))
                ),
                1,
                2
            ).build()
        )
            .distanceFromStartOfRoute(123.0)
            .info(
                IncidentInfo.Builder("some_id")
                    .type(IncidentType.CONSTRUCTION)
                    .creationTime(Date(40))
                    .startTime(Date(60))
                    .endTime(Date(80))
                    .isClosed(true)
                    .congestion(IncidentCongestion.Builder().value(4).build())
                    .impact(IncidentImpact.LOW)
                    .description("incident description")
                    .subType("incident sub-type")
                    .subTypeDescription("incident sub-type description")
                    .alertcCodes(listOf(10, 20, 30))
                    .build()
            )

    @Test
    override fun trigger() {
        // see docs
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val incident = Incident.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, incident.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val incident = Incident.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, incident.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val incident = Incident.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, incident.distanceFromStartOfRoute)
    }
}
