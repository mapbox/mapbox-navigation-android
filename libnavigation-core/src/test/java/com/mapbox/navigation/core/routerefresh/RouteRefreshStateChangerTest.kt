package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.reflect.full.memberProperties

@RunWith(Parameterized::class)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshStateChangerTest(
    private val from: String?,
    private val to: String?,
    private val expected: Boolean,
) {

    companion object {

        @Parameterized.Parameters(name = "from {0} to {1} should be {2}")
        @JvmStatic
        fun data(): Collection<Array<Any?>> {
            val result = listOf<Array<Any?>>(
                arrayOf(null, null, false),
                arrayOf(null, RouteRefreshExtra.REFRESH_STATE_STARTED, true),
                arrayOf(null, RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED, true),
                arrayOf(null, RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS, false),
                arrayOf(null, RouteRefreshExtra.REFRESH_STATE_CANCELED, false),
                arrayOf(RouteRefreshExtra.REFRESH_STATE_STARTED, null, true),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    false
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    true
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    true
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    true
                ),
                arrayOf(RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED, null, true),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    true
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    false
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    false
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    false
                ),
                arrayOf(RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS, null, true),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    true
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    false
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    false
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    false
                ),
                arrayOf(RouteRefreshExtra.REFRESH_STATE_CANCELED, null, true),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    true
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    true
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    false
                ),
                arrayOf(
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    RouteRefreshExtra.REFRESH_STATE_CANCELED,
                    false
                ),
            )
            val expectedNumberOfStates = getExpectedNumberOfStates()
            assertEquals(expectedNumberOfStates * expectedNumberOfStates, result.size)
            return result
        }

        private fun getExpectedNumberOfStates(): Int {
            return RouteRefreshExtra::class.memberProperties.size + 1 // 1 for null
        }
    }

    @Test
    fun canChange() {
        assertEquals(expected, RouteRefreshStateChanger.canChange(from, to))
    }
}
