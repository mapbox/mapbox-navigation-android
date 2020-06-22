package com.mapbox.navigation.examples

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.mapbox.navigation.examples.ui.BuildingExtrusionHighlightActivity
import com.mapbox.navigation.examples.ui.BuildingFootprintHighlightActivity
import com.mapbox.navigation.examples.ui.CustomCameraActivity
import com.mapbox.navigation.examples.ui.CustomPuckActivity
import com.mapbox.navigation.examples.ui.CustomUIComponentStyleActivity
import com.mapbox.navigation.examples.ui.NavigationViewActivity
import com.mapbox.navigation.examples.ui.NavigationViewFragmentActivity
import kotlinx.android.synthetic.main.activity_ui.*

class UIActivity : AppCompatActivity() {

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: ExamplesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ui)

        val sampleItemList = buildSampleList()
        adapter = ExamplesAdapter(this) {
            startActivity(Intent(this@UIActivity, sampleItemList[it].activity))
        }
        layoutManager = LinearLayoutManager(this, VERTICAL, false)
        uiRecycler.layoutManager = layoutManager
        uiRecycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        uiRecycler.adapter = adapter
        adapter.addSampleItems(sampleItemList)
    }

    private fun buildSampleList(): List<SampleItem> {
        // Return list of all activities demonstrating UI SDK capabilities
        return listOf(
            SampleItem(
                    getString(R.string.title_navigation_view),
                    getString(R.string.description_navigation_view),
                    NavigationViewActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_navigation_view_fragment),
                getString(R.string.description_navigation_view_fragment),
                NavigationViewFragmentActivity::class.java
            ),
            SampleItem(
                    getString(R.string.title_building_highlight_kotlin),
                    getString(R.string.description_building_highlight_kotlin),
                    BuildingFootprintHighlightActivity::class.java
            ),
            SampleItem(
                    getString(R.string.title_ui_building_extrusions_kotlin),
                    getString(R.string.description_ui_building_extrusions_kotlin),
                    BuildingExtrusionHighlightActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_custom_puck_example),
                getString(R.string.description_custom_puck_example),
                CustomPuckActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_custom_camera_example),
                getString(R.string.description_custom_camera_example),
                CustomCameraActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_custom_ui_component_style),
                getString(R.string.description_custom_ui_component_style),
                CustomUIComponentStyleActivity::class.java
            )
        )
    }
}
