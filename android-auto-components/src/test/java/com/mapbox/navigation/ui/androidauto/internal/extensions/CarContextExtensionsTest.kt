package com.mapbox.navigation.ui.androidauto.internal.extensions

import androidx.car.app.CarContext
import androidx.car.app.HostException
import androidx.car.app.constraints.ConstraintManager
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CarContextExtensionsTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val constraintManager: ConstraintManager = mockk()
    private val carContext: CarContext = mockk {
        every { getCarService(ConstraintManager::class.java) } returns constraintManager
    }

    @Test
    fun `the content limit comes from the host`() {
        every {
            constraintManager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
        } returns 6

        assertEquals(6, carContext.contentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST))
    }

    @Test
    fun `a host failure means no limit`() {
        every {
            constraintManager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
        } throws HostException("host unbound")

        assertEquals(
            Int.MAX_VALUE,
            carContext.contentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST),
        )
    }
}
