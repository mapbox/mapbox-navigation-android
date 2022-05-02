package com.mapbox.androidauto.deeplink

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class GeoDeeplinkParserTest {

    @Test
    fun `destination erases when it is gotten`() {
        GeoDeeplinkParser.parseAndSave("geo:37.788151,-122.407543")

        assertEquals(true, GeoDeeplinkParser.destinationReceiveChannel.tryReceive().isSuccess)
        assertEquals(true, GeoDeeplinkParser.destinationReceiveChannel.tryReceive().isFailure)
    }
}
