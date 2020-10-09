package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class IncidentCongestionTest : BuilderTest<IncidentCongestion, IncidentCongestion.Builder>() {
    override fun getImplementationClass(): KClass<IncidentCongestion> = IncidentCongestion::class

    override fun getFilledUpBuilder(): IncidentCongestion.Builder =
        IncidentCongestion.Builder().value(30)

    @Test
    override fun trigger() {
        // read doc
    }
}
