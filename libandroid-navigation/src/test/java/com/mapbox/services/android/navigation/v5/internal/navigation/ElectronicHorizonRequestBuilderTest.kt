package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ElectronicHorizonRequestBuilderTest(
    private val params: TestParams
) {
    @Test
    fun `checks ElectronicHorizonRequestBuilder`() {
        val actualResult = ElectronicHorizonRequestBuilder.build(params.expansion, params.locations)
        Assert.assertEquals(params.expectedResult, actualResult)
    }

    internal data class TestParams(
        val expansion: ElectronicHorizonRequestBuilder.Expansion,
        val locations: List<Location>,
        val expectedResult: String
    ) {
        override fun toString(): String {
            return "Run test with extension = $expansion, locations = ${locations.map { "lat = ${it.latitude}, lon = ${it.longitude}" }}"
        }
    }

    private companion object {
        private const val DEFAULT_LATITUDE_1 = 10.0
        private const val DEFAULT_LATITUDE_2 = 20.0
        private const val DEFAULT_LATITUDE_3 = 30.0

        private const val DEFAULT_LONGITUDE_1 = 111.0
        private const val DEFAULT_LONGITUDE_2 = 222.0
        private const val DEFAULT_LONGITUDE_3 = 333.0

        private fun mockLocation(latitude: Double, longitude: Double): Location {
            return mockk<Location>().also {
                every { it.latitude }.returns(latitude)
                every { it.longitude }.returns(longitude)
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<TestParams> {
            return listOf(
                TestParams(
                    ElectronicHorizonRequestBuilder.Expansion._1D,
                    emptyList(),
                    "{\"shape\":[],\"eh_options\":{\"expansion\":\"1D\"}}"
                ),
                TestParams(
                    ElectronicHorizonRequestBuilder.Expansion._1_5D,
                    emptyList(),
                    "{\"shape\":[],\"eh_options\":{\"expansion\":\"1.5D\"}}"
                ),
                TestParams(
                    ElectronicHorizonRequestBuilder.Expansion._2D,
                    emptyList(),
                    "{\"shape\":[],\"eh_options\":{\"expansion\":\"2D\"}}"
                ),
                TestParams(
                    ElectronicHorizonRequestBuilder.Expansion._1D,
                    listOf(
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_1,
                            longitude = DEFAULT_LONGITUDE_1
                        ),
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_2,
                            longitude = DEFAULT_LONGITUDE_2
                        ),
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_3,
                            longitude = DEFAULT_LONGITUDE_3
                        )
                    ),
                    "{\"shape\":[{\"lat\":10.0,\"lon\":111.0},{\"lat\":20.0,\"lon\":222.0},{\"lat\":30.0,\"lon\":333.0}],\"eh_options\":{\"expansion\":\"1D\"}}"
                ),
                TestParams(
                    ElectronicHorizonRequestBuilder.Expansion._1_5D,
                    listOf(
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_1,
                            longitude = DEFAULT_LONGITUDE_1
                        ),
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_2,
                            longitude = DEFAULT_LONGITUDE_2
                        ),
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_3,
                            longitude = DEFAULT_LONGITUDE_3
                        )
                    ),
                    "{\"shape\":[{\"lat\":10.0,\"lon\":111.0},{\"lat\":20.0,\"lon\":222.0},{\"lat\":30.0,\"lon\":333.0}],\"eh_options\":{\"expansion\":\"1.5D\"}}"
                ),
                TestParams(
                    ElectronicHorizonRequestBuilder.Expansion._2D,
                    listOf(
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_1,
                            longitude = DEFAULT_LONGITUDE_1
                        ),
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_2,
                            longitude = DEFAULT_LONGITUDE_2
                        ),
                        mockLocation(
                            latitude = DEFAULT_LATITUDE_3,
                            longitude = DEFAULT_LONGITUDE_3
                        )
                    ),
                    "{\"shape\":[{\"lat\":10.0,\"lon\":111.0},{\"lat\":20.0,\"lon\":222.0},{\"lat\":30.0,\"lon\":333.0}],\"eh_options\":{\"expansion\":\"2D\"}}"
                )
            )
        }
    }
}
