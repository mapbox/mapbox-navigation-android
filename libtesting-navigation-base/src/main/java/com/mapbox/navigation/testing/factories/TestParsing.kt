package com.mapbox.navigation.testing.factories

import com.mapbox.common.dispatchers.SdkDispatchers
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchingMatchParser
import com.mapbox.navigation.base.internal.route.parsing.noTracking
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.utils.internal.Time
import kotlinx.coroutines.CoroutineDispatcher

fun createTestNavigationRoutesParsing(
    parsingDispatcher: CoroutineDispatcher = SdkDispatchers.Default,
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

fun createTestMapMatchingResponseParser(
    parsingDispatcher: CoroutineDispatcher = SdkDispatchers.Default,
): MapMatchingMatchParser = setupTestParsing(
    testParsingDispatcher = parsingDispatcher,
)

private fun setupTestParsing(
    testTime: Time = Time.SystemClockImpl,
    testParsingDispatcher: CoroutineDispatcher = SdkDispatchers.Default
) = setupParsing(
    false,
    time = testTime,
    noTracking(),
    testParsingDispatcher,
    { null },
    TestSDKRouteParser()
)
