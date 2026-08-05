package com.mapbox.navigation.ui.androidauto.testing

import org.junit.Ignore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Use a single sdk configuration for testing car screens.
 * This will keep tests fast.
 */
@Ignore("Used for enabling Robolectric")
@RunWith(RobolectricTestRunner::class)
open class MapboxRobolectricTestRunner
