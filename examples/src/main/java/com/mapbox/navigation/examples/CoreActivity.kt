package com.mapbox.navigation.examples

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.mapbox.navigation.examples.core.BasicNavSdkOnlyActivity
import com.mapbox.navigation.examples.core.BasicNavigationActivity
import com.mapbox.navigation.examples.core.BasicNavigationFragmentActivity
import com.mapbox.navigation.examples.core.CustomAlternativeRouteClickPaddingActivity
import com.mapbox.navigation.examples.core.CustomRouteStylingActivity
import com.mapbox.navigation.examples.core.DebugMapboxNavigationKt
import com.mapbox.navigation.examples.core.FasterRouteActivity
import com.mapbox.navigation.examples.core.FeedbackButtonActivity
import com.mapbox.navigation.examples.core.FreeDriveNavigationActivity
import com.mapbox.navigation.examples.core.GuidanceViewActivity
import com.mapbox.navigation.examples.core.InstructionViewActivity
import com.mapbox.navigation.examples.core.JunctionSnapshotActivity
import com.mapbox.navigation.examples.core.MapMatchingActivity
import com.mapbox.navigation.examples.core.NavigationMapRouteActivity
import com.mapbox.navigation.examples.core.ReRouteActivity
import com.mapbox.navigation.examples.core.ReplayActivity
import com.mapbox.navigation.examples.core.ReplayHistoryActivity
import com.mapbox.navigation.examples.core.ReplayWaypointsActivity
import com.mapbox.navigation.examples.core.RouteAlertsActivity
import com.mapbox.navigation.examples.core.RuntimeRouteStylingActivity
import com.mapbox.navigation.examples.core.SilentWaypointsRerouteActivity
import com.mapbox.navigation.examples.core.SimpleMapboxNavigationKt
import com.mapbox.navigation.examples.core.SummaryBottomSheetActivity
import kotlinx.android.synthetic.main.activity_core.*

class CoreActivity : AppCompatActivity() {

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: ExamplesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_core)

        val sampleItemList = buildSampleList()
        adapter = ExamplesAdapter(this) {
            startActivity(Intent(this@CoreActivity, sampleItemList[it].activity))
        }
        layoutManager = LinearLayoutManager(this, VERTICAL, false)
        coreRecycler.layoutManager = layoutManager
        coreRecycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        coreRecycler.adapter = adapter
        adapter.addSampleItems(sampleItemList)
    }

    private fun buildSampleList(): List<SampleItem> {
        return listOf(
            SampleItem(
                getString(R.string.title_simple_navigation_kotlin),
                getString(R.string.description_simple_navigation_kotlin),
                SimpleMapboxNavigationKt::class.java
            ),
            SampleItem(
                getString(R.string.title_guidance_view),
                getString(R.string.description_guidance_view),
                GuidanceViewActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_junction_snapshot_sample),
                getString(R.string.description_junction_snapshot_sample),
                JunctionSnapshotActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_reroute_view),
                getString(R.string.description_reroute_view),
                ReRouteActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_basic_navigation_kotlin),
                getString(R.string.description_basic_navigation_kotlin),
                BasicNavigationActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_basic_navigation_fragment),
                getString(R.string.description_basic_navigation_fragment),
                BasicNavigationFragmentActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_basic_navigation_sdk_only_kotlin),
                getString(R.string.description_basic_navigation_sdk_only_kotlin),
                BasicNavSdkOnlyActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_free_drive_kotlin),
                getString(R.string.description_free_drive_kotlin),
                FreeDriveNavigationActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_replay_route),
                getString(R.string.description_replay_route),
                ReplayActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_replay_history_kotlin),
                getString(R.string.description_replay_history_kotlin),
                ReplayHistoryActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_replay_waypoints),
                getString(R.string.description_replay_waypoints),
                ReplayWaypointsActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_silent_waypoints_reroute),
                getString(R.string.description_silent_waypoints_reroute),
                SilentWaypointsRerouteActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_faster_route),
                getString(R.string.description_faster_route),
                FasterRouteActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_instruction_view),
                getString(R.string.description_instruction_view),
                InstructionViewActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_summary_bottom_sheet),
                getString(R.string.description_summary_bottom_sheet),
                SummaryBottomSheetActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_feedback_button),
                getString(R.string.description_feedback_button),
                FeedbackButtonActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_debug_navigation_kotlin),
                getString(R.string.description_debug_navigation_kotlin),
                DebugMapboxNavigationKt::class.java
            ),
            SampleItem(
                getString(R.string.title_custom_route_styling_kotlin),
                getString(R.string.description_custom_route_styling_kotlin),
                CustomRouteStylingActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_navigation_route_ui),
                getString(R.string.description_navigation_route_ui),
                NavigationMapRouteActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_runtime_styling),
                getString(R.string.description_runtime_styling),
                RuntimeRouteStylingActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_map_matching),
                getString(R.string.description_map_matching),
                MapMatchingActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_route_alerts),
                getString(R.string.description_route_alerts),
                RouteAlertsActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_alternative_route_custom_click_padding),
                getString(R.string.description_alternative_route_custom_click_padding),
                CustomAlternativeRouteClickPaddingActivity::class.java
            )

        )
    }
}
