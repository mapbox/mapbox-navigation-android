package com.mapbox.navigation.route.offboard

import androidx.test.core.app.launchActivity
import com.mapbox.navigation.route.offboard.test.OffboardRouterTestActivity
import com.mapbox.navigation.route.offboard.test.OffboardRouterTestActivity.Companion.ROUTE_READY
import com.mapbox.navigation.route.offboard.test.OffboardRouterTestActivity.Companion.ROUTE_RESULT
import org.junit.Assert.assertEquals
import org.junit.Test

class OffboardRouterTest {

    @Test
    fun successRouteFetching() {
        val activityScenario = launchActivity<OffboardRouterTestActivity>()
        activityScenario.onActivity { activity ->
            activity.checkRouteFetching()
        }

        activityScenario.result.let {
            val result = it.resultData.getSerializableExtra(ROUTE_RESULT)
            assertEquals(ROUTE_READY, result)
        }
    }
}
