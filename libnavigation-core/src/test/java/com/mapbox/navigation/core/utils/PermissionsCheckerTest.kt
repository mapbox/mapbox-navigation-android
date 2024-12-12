package com.mapbox.navigation.core.utils

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.FOREGROUND_SERVICE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PermissionsCheckerTest(private val param: Param) {

    @Test
    fun test() {
        mockkStatic(ContextCompat::class)

        val context = mockk<Context>()
        param.grantedPermissions.forEach { permission ->
            every {
                ContextCompat.checkSelfPermission(eq(context), eq(permission))
            } returns PackageManager.PERMISSION_GRANTED
        }
        ALL_PERMISSIONS.toMutableList().apply {
            removeAll(param.grantedPermissions)
            forEach { permission ->
                every {
                    ContextCompat.checkSelfPermission(eq(context), eq(permission))
                } returns PackageManager.PERMISSION_DENIED
            }
        }

        val permissionsChecker = PermissionsChecker(context) {
            param.apiLevel
        }

        assertEquals(
            param.checkResult,
            permissionsChecker.hasForegroundServiceLocationPermissions(),
        )

        unmockkStatic(ContextCompat::class)
    }

    data class Param(
        val apiLevel: Int,
        val grantedPermissions: List<String>,
        val checkResult: Expected<String, Unit>,
    )

    private fun <E, V> assertEquals(e1: Expected<E, V>, e2: Expected<E, V>) {
        if (e1.isValue) {
            if (e2.isValue) {
                assertEquals(e1.value, e2.value)
            } else {
                fail("Not equals: value ${e1.value}, error: ${e2.error}")
            }
        } else {
            if (e2.isError) {
                assertEquals(e1.error, e2.error)
            } else {
                fail("Not equals: error ${e1.error}, value: ${e2.value}")
            }
        }
    }

    companion object {

        val ALL_PERMISSIONS = listOf(
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION,
            FOREGROUND_SERVICE_LOCATION,
        )

        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): List<Param> = listOf(
            Param(1, emptyList(), ExpectedFactory.createValue(Unit)),
            Param(33, emptyList(), ExpectedFactory.createValue(Unit)),
            Param(
                34,
                emptyList(),
                ExpectedFactory.createError(
                    "Missing permissions: $FOREGROUND_SERVICE_LOCATION, " +
                        "Any of $ACCESS_FINE_LOCATION or $ACCESS_COARSE_LOCATION",
                ),
            ),
            Param(
                34,
                listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),
                ExpectedFactory.createError("Missing permissions: $FOREGROUND_SERVICE_LOCATION"),
            ),
            Param(
                34,
                listOf(FOREGROUND_SERVICE_LOCATION),
                ExpectedFactory.createError(
                    "Missing permissions: Any of $ACCESS_FINE_LOCATION or $ACCESS_COARSE_LOCATION",
                ),
            ),
            Param(
                34,
                listOf(ACCESS_COARSE_LOCATION, FOREGROUND_SERVICE_LOCATION),
                ExpectedFactory.createValue(Unit),
            ),
            Param(
                34,
                listOf(ACCESS_FINE_LOCATION, FOREGROUND_SERVICE_LOCATION),
                ExpectedFactory.createValue(Unit),
            ),
            Param(
                34,
                listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, FOREGROUND_SERVICE_LOCATION),
                ExpectedFactory.createValue(Unit),
            ),
            Param(
                100,
                listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, FOREGROUND_SERVICE_LOCATION),
                ExpectedFactory.createValue(Unit),
            ),
        )
    }
}
