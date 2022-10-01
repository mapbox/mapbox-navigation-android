package com.mapbox.androidauto.car.settings

import android.content.Context
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.test.core.app.ApplicationProvider
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.androidauto.testing.TestOnDoneCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class CarSettingsScreenTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockCarSettingsStorage: CarSettingsStorage = mockk(relaxUnitFun = true)
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { carContext } returns mockk {
            every { getString(any()) } answers {
                context.getString(args[0].toString().toInt())
            }
        }
        every { carSettingsStorage } returns mockCarSettingsStorage
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
}
