package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import java.util.Date
import kotlin.reflect.KClass

class IncidentInfoTest : BuilderTest<IncidentInfo, IncidentInfo.Builder>() {

    override fun getImplementationClass(): KClass<IncidentInfo> = IncidentInfo::class

    override fun getFilledUpBuilder(): IncidentInfo.Builder =
        IncidentInfo.Builder("id_12123dsa")
            .type(IncidentType.ACCIDENT)
            .creationTime(Date(10))
            .startTime(Date(32323))
            .endTime(Date(41321231))
            .isClosed(true)
            .congestion(IncidentCongestion.Builder().value(32).build())
            .impact(IncidentImpact.CRITICAL)
            .description("incident description blah blah ")
            .subType("incident sub-type blah blah")
            .subTypeDescription("incident sub-type description description")
            .alertcCodes(listOf(120, 320, 12))

    @Test
    override fun trigger() {
        // read doc
    }
}
