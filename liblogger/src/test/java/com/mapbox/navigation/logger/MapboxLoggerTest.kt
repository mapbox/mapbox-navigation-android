package com.mapbox.navigation.logger

import com.mapbox.navigation.base.logger.Logger
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxLoggerTest {

    private lateinit var logger: Logger

    @Before
    fun setUp() {
        logger = MapboxLogger()
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(logger)
    }
}
