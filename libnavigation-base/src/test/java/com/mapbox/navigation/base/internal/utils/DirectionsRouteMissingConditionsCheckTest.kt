package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.net.URL

class DirectionsRouteMissingConditionsCheckTest {

    private val exclusiveQueries: Map<String, String> = mapOf(
        "engine" to "electric",
        "key1" to "value1",
    )

    @Before
    fun setup() {
        mockkObject(QueriesProvider)
        every { QueriesProvider.exclusiveQueries } returns exclusiveQueries
    }

    @After
    fun cleanup() {
        unmockkObject(QueriesProvider)
    }

    @Test
    fun cases() {
        listOf(
            CasesWrapper(
                "Not restricted key-value pair",
                "https://test.com/?key0=value0",
                CasesWrapper.Result.Success,
            ),
            CasesWrapper(
                "Restricted key-value pair: key1=value1",
                "https://test.com/?key1=value1",
                CasesWrapper.Result.Error("[(key1=value1)]"),
            ),
            CasesWrapper(
                "Restricted key-value pair with other valid queries: engine=electric",
                "https://test.com/?key0=value0&engine=electric&key2=value2",
                CasesWrapper.Result.Error("[(engine=electric)]"),
            ),
            CasesWrapper(
                "Restricted key and valid value: engine=nonelectric",
                "https://test.com/?engine=nonelectric",
                CasesWrapper.Result.Success,
            ),
            CasesWrapper(
                "Valid key and restricted value: machine=electric",
                "https://test.com/?key0=value0&machine=electric",
                CasesWrapper.Result.Success,
            ),
            CasesWrapper(
                "Valid key and restricted value and restricted key and valid value: " +
                    "machine=electric & engine=nonelectric",
                "https://test.com/?machine=electric&engine=nonelectric",
                CasesWrapper.Result.Success,
            ),
            CasesWrapper(
                "A few restricted key and valid values: engine=electric " +
                    "and key1=value1",
                "https://test.com/?key0=value0&engine=electric&key1=value1",
                CasesWrapper.Result.Error("[(engine=electric);(key1=value1)]"),
            ),
        ).forEach { (description, url, result) ->
            val directionRoute = mockk<DirectionsRoute> {
                every { routeOptions()?.toUrl(any()) } returns URL(url).also {
                    assertEquals(url, it.toString())
                }
            }

            when (result) {
                is CasesWrapper.Result.Error -> {
                    val exception =
                        assertThrows(description, IllegalStateException::class.java) {
                            DirectionsRouteMissingConditionsCheck.checkDirectionsRoute(
                                directionRoute,
                            )
                        }
                    assertTrue(
                        description,
                        exception.message!!.startsWith(
                            DirectionsRouteMissingConditionsCheck.ERROR_MESSAGE_TEMPLATE,
                        ),
                    )
                    assertTrue(
                        description,
                        exception.message!!.contains(result.errorKeyValuePairs),
                    )
                }
                CasesWrapper.Result.Success -> {
                    DirectionsRouteMissingConditionsCheck.checkDirectionsRoute(directionRoute)
                }
            }
        }
    }

    private data class CasesWrapper(
        val description: String,
        val url: String,
        val result: Result,
    ) {
        sealed class Result {
            object Success : Result()
            data class Error(val errorKeyValuePairs: String) : Result()
        }
    }
}
