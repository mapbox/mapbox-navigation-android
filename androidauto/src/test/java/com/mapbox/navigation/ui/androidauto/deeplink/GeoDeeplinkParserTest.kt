package com.mapbox.navigation.ui.androidauto.deeplink

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoDeeplinkParserTest {

    @Test
    fun `destination erases when it is gotten`() {
        GeoDeeplinkParser.parseAndSave("geo:37.788151,-122.407543")

        assertEquals(true, GeoDeeplinkParser.destinationReceiveChannel.tryReceive().isSuccess)
        assertEquals(true, GeoDeeplinkParser.destinationReceiveChannel.tryReceive().isFailure)
    }
}
