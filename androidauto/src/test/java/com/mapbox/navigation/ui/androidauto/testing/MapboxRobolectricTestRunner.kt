package com.mapbox.navigation.ui.androidauto.testing

import android.os.Build
import org.junit.Ignore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Use a single sdk configuration for testing car screens.
 * This will keep tests fast.
 */
@Ignore("Used for enabling Robolectric")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
open class MapboxRobolectricTestRunner
