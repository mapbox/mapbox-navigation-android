package com.mapbox.navigation.ui.androidauto.notification

import androidx.car.app.notification.CarAppExtender
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class FreeDriveExtenderUpdaterTest : MapboxRobolectricTestRunner() {

    private val sut = FreeDriveExtenderUpdater(ApplicationProvider.getApplicationContext())

    @Test
    fun `icon is rendered once and reused`() {
        val first = CarAppExtender.Builder().also { sut.update(it) }.build().largeIcon
        val second = CarAppExtender.Builder().also { sut.update(it) }.build().largeIcon

        assertNotNull(first)
        assertSame(first, second)
    }
}
