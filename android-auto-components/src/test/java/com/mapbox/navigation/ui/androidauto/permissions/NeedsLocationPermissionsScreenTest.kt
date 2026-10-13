package com.mapbox.navigation.ui.androidauto.permissions

import android.content.Context
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.model.MessageTemplate
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeedsLocationPermissionsScreenTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var apiLevel = 7
    private val carContext: CarContext = mockk(relaxed = true) {
        every { getString(any()) } answers { context.getString(firstArg()) }
        every { getCarService(AppManager::class.java) } returns mockk(relaxed = true)
        every { carAppApiLevel } answers { apiLevel }
    }

    // Which setters are used on each Car API level is covered by CarTemplateHeadersTest. The
    // library returns the same header and legacy fields from either, so this pins the result.
    @Suppress("DEPRECATION")
    @Test
    fun `template shows the title without back on every Car API level`() {
        listOf(3, 6, 7).forEach { level ->
            apiLevel = level

            val template = NeedsLocationPermissionsScreen(carContext).onGetTemplate()
                as MessageTemplate

            assertEquals("Needs location permission", template.header!!.title.toString())
            assertNull(template.header!!.startHeaderAction)
            assertEquals("Needs location permission", template.title.toString())
            assertNull(template.headerAction)
            assertEquals("OK", template.actions.single().title.toString())
        }
    }
}
