package com.mapbox.navigation.ui.maps.roadname.view

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.roadname.model.RoadLabel
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRoadNameLabelViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when road label contains road name and shield then show shield`() {
        val roadLabel = RoadLabel("Central Avenue", byteArrayOf(), "101 South")
        val view = MapboxRoadNameLabelView(ctx)
        val expected = View.VISIBLE

        view.render(roadLabel)

        assertEquals(expected, view.findViewById<ImageView>(R.id.roadNameShieldIcon).visibility)
    }

    @Test
    fun `when road label contains only road name then hide shield`() {
        val roadLabel = RoadLabel("Central Avenue", null, "101 South")
        val view = MapboxRoadNameLabelView(ctx)
        val expected = View.GONE

        view.render(roadLabel)

        assertEquals(expected, view.findViewById<ImageView>(R.id.roadNameShieldIcon).visibility)
    }

    @Test
    fun `when road label contains road name then render the text`() {
        val roadLabel = RoadLabel("Central Avenue", null, "101 South")
        val view = MapboxRoadNameLabelView(ctx)
        val textView = view.findViewById<TextView>(R.id.roadNameLabel)
        val expected = "Central Avenue"

        view.render(roadLabel)

        assertEquals(expected, textView.text)
    }

    @Test
    fun `when show shield icon invoked with true then icon should show`() {
        val roadLabel = RoadLabel("Central Avenue", null, "101 South")
        val view = MapboxRoadNameLabelView(ctx)
        val expected = View.VISIBLE

        view.render(roadLabel)
        view.showShieldIcon(true)

        assertEquals(expected, view.findViewById<ImageView>(R.id.roadNameShieldIcon).visibility)
    }

    @Test
    fun `when show shield icon invoked with false then icon should hide`() {
        val roadLabel = RoadLabel("Central Avenue", null, "101 South")
        val view = MapboxRoadNameLabelView(ctx)
        val expected = View.GONE

        view.render(roadLabel)
        view.showShieldIcon(false)

        assertEquals(expected, view.findViewById<ImageView>(R.id.roadNameShieldIcon).visibility)
    }

    @Test
    fun `when show shield name invoked with true then text should show`() {
        val roadLabel = RoadLabel("Central Avenue", null, "101 South")
        val view = MapboxRoadNameLabelView(ctx)
        val expected = View.VISIBLE

        view.render(roadLabel)
        view.showRoadName(true)

        assertEquals(expected, view.findViewById<TextView>(R.id.roadNameLabel).visibility)
    }

    @Test
    fun `when show shield name invoked with false then text should hide`() {
        val roadLabel = RoadLabel("Central Avenue", null, "101 South")
        val view = MapboxRoadNameLabelView(ctx)
        val expected = View.GONE

        view.render(roadLabel)
        view.showRoadName(false)

        assertEquals(expected, view.findViewById<TextView>(R.id.roadNameLabel).visibility)
    }
}
