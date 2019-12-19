package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.services.android.navigation.v5.internal.navigation.ElectronicHorizonParams.Builder.Companion.DEFAULT_ELECTRONIC_HORIZON_DELAY
import com.mapbox.services.android.navigation.v5.internal.navigation.ElectronicHorizonParams.Builder.Companion.DEFAULT_ELECTRONIC_HORIZON_INTERVAL
import com.mapbox.services.android.navigation.v5.internal.navigation.ElectronicHorizonParams.Builder.Companion.DEFAULT_LOCATIONS_CACHE_SIZE
import com.mapbox.services.android.navigation.v5.internal.navigation.ElectronicHorizonParams.Builder.Companion.LOCATIONS_CACHE_MAX_SIZE
import com.mapbox.services.android.navigation.v5.internal.navigation.ElectronicHorizonParams.Builder.Companion.LOCATIONS_CACHE_MIN_SIZE
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ElectronicHorizonParamsTest(
    private val testData: TestData
) {
    @Test
    fun `checks ElectronicHorizonParamsBuilder`() {
        val initParams = testData.params
        val expectedResults = testData.results

        val actualParams = ElectronicHorizonParams.Builder()
            .delay(initParams.delay)
            .interval(initParams.interval)
            .locationsCacheSize(initParams.locationsCacheSize)
            .build()

        Assert.assertEquals(expectedResults.delay, actualParams.delay)
        Assert.assertEquals(expectedResults.interval, actualParams.interval)
        Assert.assertEquals(expectedResults.locationsCacheSize, actualParams.locationsCacheSize)
    }

    internal data class TestData(
        val params: TestParams,
        val results: ExpectedResults
    )

    internal data class TestParams(
        val delay: Long = 0,
        val interval: Long = 0,
        val locationsCacheSize: Int = 0
    )

    internal data class ExpectedResults(
        val delay: Long = DEFAULT_ELECTRONIC_HORIZON_DELAY,
        val interval: Long = DEFAULT_ELECTRONIC_HORIZON_INTERVAL,
        val locationsCacheSize: Int = DEFAULT_LOCATIONS_CACHE_SIZE
    )

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<TestData> {
            return listOf(
                TestData(
                    TestParams(),
                    ExpectedResults(
                        delay = DEFAULT_ELECTRONIC_HORIZON_DELAY,
                        interval = DEFAULT_ELECTRONIC_HORIZON_INTERVAL,
                        locationsCacheSize = DEFAULT_LOCATIONS_CACHE_SIZE
                    )
                ),

                TestData(
                    TestParams(delay = -1),
                    ExpectedResults(delay = DEFAULT_ELECTRONIC_HORIZON_DELAY)

                ),

                TestData(
                    TestParams(delay = 0),
                    ExpectedResults(delay = DEFAULT_ELECTRONIC_HORIZON_DELAY)
                ),

                TestData(
                    TestParams(delay = 1),
                    ExpectedResults(delay = 1)
                ),

                TestData(
                    TestParams(interval = -1),
                    ExpectedResults(interval = DEFAULT_ELECTRONIC_HORIZON_INTERVAL)

                ),

                TestData(
                    TestParams(interval = 0),
                    ExpectedResults(interval = DEFAULT_ELECTRONIC_HORIZON_INTERVAL)
                ),

                TestData(
                    TestParams(interval = 1),
                    ExpectedResults(interval = 1)
                ),

                TestData(
                    TestParams(locationsCacheSize = -1),
                    ExpectedResults(locationsCacheSize = DEFAULT_LOCATIONS_CACHE_SIZE)

                ),

                TestData(
                    TestParams(locationsCacheSize = 0),
                    ExpectedResults(locationsCacheSize = DEFAULT_LOCATIONS_CACHE_SIZE)
                ),

                TestData(
                    TestParams(locationsCacheSize = LOCATIONS_CACHE_MIN_SIZE - 1),
                    ExpectedResults(locationsCacheSize = DEFAULT_LOCATIONS_CACHE_SIZE)
                ),

                TestData(
                    TestParams(locationsCacheSize = LOCATIONS_CACHE_MIN_SIZE),
                    ExpectedResults(locationsCacheSize = LOCATIONS_CACHE_MIN_SIZE)
                ),
                TestData(
                    TestParams(locationsCacheSize = DEFAULT_LOCATIONS_CACHE_SIZE),
                    ExpectedResults(locationsCacheSize = DEFAULT_LOCATIONS_CACHE_SIZE)
                ),
                TestData(
                    TestParams(locationsCacheSize = LOCATIONS_CACHE_MAX_SIZE + 1),
                    ExpectedResults(locationsCacheSize = DEFAULT_LOCATIONS_CACHE_SIZE)
                )
            )
        }
    }
}
