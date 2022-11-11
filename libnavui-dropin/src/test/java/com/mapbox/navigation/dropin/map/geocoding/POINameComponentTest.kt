package com.mapbox.navigation.dropin.map.geocoding

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class POINameComponentTest {
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var textView: AppCompatTextView
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: POINameComponent

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mapboxNavigation = mockk(relaxed = true)
        store = spyk(TestStore())
        textView = AppCompatTextView(context)

        sut = POINameComponent(store, textView, MutableStateFlow(0))
    }

    @Test
    fun `should update poiName text`() {
        val featurePlaceName = "POI NAME"
        val newDestination = Destination(
            Point.fromLngLat(1.0, 2.0),
            listOf(
                mockk {
                    every { placeName() } returns featurePlaceName
                }
            )
        )

        sut.onAttached(mockk())
        store.setState(
            State(
                destination = newDestination
            )
        )

        assertEquals(textView.text, featurePlaceName)
    }
}
