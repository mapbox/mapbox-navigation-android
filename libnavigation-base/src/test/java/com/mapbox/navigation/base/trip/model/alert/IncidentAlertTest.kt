package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import java.util.Date
import kotlin.reflect.KClass

class IncidentAlertTest : BuilderTest<IncidentAlert, IncidentAlert.Builder>() {

    override fun getImplementationClass(): KClass<IncidentAlert> = IncidentAlert::class

    override fun getFilledUpBuilder(): IncidentAlert.Builder =
        IncidentAlert.Builder(

            Point.fromLngLat(10.0, 20.0),
            123.0
        )
            .alertGeometry(
                RouteAlertGeometry.Builder(
                    456.0,
                    Point.fromLngLat(10.0, 20.0),
                    1,
                    Point.fromLngLat(33.0, 44.0),
                    2
                ).build()
            )
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
}
