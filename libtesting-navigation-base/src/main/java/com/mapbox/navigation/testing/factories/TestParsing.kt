package com.mapbox.navigation.testing.factories

import com.mapbox.navigation.base.internal.route.parsing.noTracking
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.utils.internal.Time
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun createTestNavigationRoutesParsing(
    parsingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    time: Time = Time.SystemClockImpl
) = setupTestParsing(
    testParsingDispatcher = parsingDispatcher,
    testTime = time,
)

fun createTestRouteInterfaceParser(
    parsingDispatcher: CoroutineDispatcher,
) = setupTestParsing(
    testParsingDispatcher = parsingDispatcher,
)

private fun setupTestParsing(
    testTime: Time = Time.SystemClockImpl,
    testParsingDispatcher: CoroutineDispatcher = Dispatchers.Default
) = setupParsing(
    false,
    time = testTime,
    noTracking(),
    testParsingDispatcher,
    { null },
    TestSDKRouteParser()
)
