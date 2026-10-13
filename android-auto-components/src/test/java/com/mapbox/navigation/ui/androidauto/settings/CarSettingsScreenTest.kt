package com.mapbox.navigation.ui.androidauto.settings

import android.content.Context
import androidx.car.app.model.Action
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.androidauto.testing.TestOnDoneCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarSettingsScreenTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var apiLevel = 7
    private val mockCarSettingsStorage: MapboxCarStorage = mockk(relaxUnitFun = true)
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { carContext } returns mockk {
            every { getString(any()) } answers {
                context.getString(args[0].toString().toInt())
            }
            every { carAppApiLevel } answers { apiLevel }
        }
        every { mapboxCarStorage } returns mockCarSettingsStorage
    }
    private val searchScreen = CarSettingsScreen(mapboxCarContext)

    @Test
    fun `replay should read last set preferences`() {
        val replayKey = context.getString(R.string.car_settings_toggle_place_holder_key)
        every { mockCarSettingsStorage.readSharedPref(replayKey, false) } returns true

        val template = searchScreen.onGetTemplate()

        val listTemplate = template as ListTemplate
        val enableReplayRow = listTemplate.singleList!!.items[0] as Row
        assertTrue(enableReplayRow.toggle!!.isChecked)
    }

    @Test
    fun `replay should be enabled after toggling`() {
        val replayKey = context.getString(R.string.car_settings_toggle_place_holder_key)
        every { mockCarSettingsStorage.readSharedPref(replayKey, false) } returns false

        val template = searchScreen.onGetTemplate()

        val testOnDoneCallback = TestOnDoneCallback()
        val enableReplayRow = (template as ListTemplate)
            .singleList!!.items[0] as Row
        enableReplayRow.toggle?.onCheckedChangeDelegate
            ?.sendCheckedChange(true, testOnDoneCallback)
        testOnDoneCallback.assertSuccess()
        verify { mockCarSettingsStorage.writeSharedPref(replayKey, true) }
    }

    // Which setters are used on each Car API level is covered by CarTemplateHeadersTest. The
    // library returns the same header and legacy fields from either, so this pins the result.
    @Suppress("DEPRECATION")
    @Test
    fun `template shows the title and back on every Car API level`() {
        every { mockCarSettingsStorage.readSharedPref(any(), false) } returns false
        listOf(3, 6, 7).forEach { level ->
            apiLevel = level

            val template = searchScreen.onGetTemplate() as ListTemplate

            assertEquals("Settings", template.header!!.title.toString())
            assertEquals(Action.BACK, template.header!!.startHeaderAction)
            assertEquals("Settings", template.title.toString())
            assertEquals(Action.BACK, template.headerAction)
        }
    }
}
