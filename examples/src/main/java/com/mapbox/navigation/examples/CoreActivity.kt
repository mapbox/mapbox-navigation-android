package com.mapbox.navigation.examples

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.mapbox.navigation.examples.core.BasicNavigationActivity
import com.mapbox.navigation.examples.core.DebugMapboxNavigationKt
import com.mapbox.navigation.examples.core.FasterRouteActivity
import com.mapbox.navigation.examples.core.FreeDriveNavigationActivity
import com.mapbox.navigation.examples.core.GuidanceViewActivity
import com.mapbox.navigation.examples.core.HybridRouterActivityJava
import com.mapbox.navigation.examples.core.HybridRouterActivityKt
import com.mapbox.navigation.examples.core.InstructionViewActivity
import com.mapbox.navigation.examples.core.OffboardRouterActivityJava
import com.mapbox.navigation.examples.core.OffboardRouterActivityKt
import com.mapbox.navigation.examples.core.OnboardRouterActivityJava
import com.mapbox.navigation.examples.core.OnboardRouterActivityKt
import com.mapbox.navigation.examples.core.ReRouteActivity
import com.mapbox.navigation.examples.core.ReplayActivity
import com.mapbox.navigation.examples.core.ReplayHistoryActivity
import com.mapbox.navigation.examples.core.SimpleMapboxNavigationKt
import com.mapbox.navigation.examples.core.SummaryBottomSheetActivity
import com.mapbox.navigation.examples.core.TripServiceActivityKt
import com.mapbox.navigation.examples.core.TripSessionActivityKt
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
                getString(R.string.title_offboard_router_java),
                getString(R.string.description_offboard_router_java),
                OffboardRouterActivityJava::class.java
            ),
            SampleItem(
                getString(R.string.title_offboard_router_kotlin),
                getString(R.string.description_offboard_router_kotlin),
                OffboardRouterActivityKt::class.java
            ),
            SampleItem(
                getString(R.string.title_onboard_router_java),
                getString(R.string.description_onboard_router_java),
                OnboardRouterActivityJava::class.java
            ),
            SampleItem(
                getString(R.string.title_onboard_router_kotlin),
                getString(R.string.description_onboard_router_kotlin),
                OnboardRouterActivityKt::class.java
            ),
            SampleItem(
                getString(R.string.title_hybrid_router_java),
                getString(R.string.description_hybrid_router_java),
                HybridRouterActivityJava::class.java
            ),
            SampleItem(
                getString(R.string.title_hybrid_router_kotlin),
                getString(R.string.description_hybrid_router_kotlin),
                HybridRouterActivityKt::class.java
            ),
            SampleItem(
                getString(R.string.title_trip_service_kotlin),
                getString(R.string.description_trip_service_kotlin),
                TripServiceActivityKt::class.java
            ),
            SampleItem(
                getString(R.string.title_trip_session_kotlin),
                getString(R.string.description_trip_session_kotlin),
                TripSessionActivityKt::class.java
            ),
            SampleItem(
                getString(R.string.title_guidance_view),
                getString(R.string.description_guidance_view),
                GuidanceViewActivity::class.java
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
                getString(R.string.title_free_drive_kotlin),
                getString(R.string.description_free_drive_kotlin),
                FreeDriveNavigationActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_replay_navigation_kotlin),
                getString(R.string.description_replay_navigation_kotlin),
                ReplayActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_replay_history_kotlin),
                getString(R.string.description_replay_history_kotlin),
                ReplayHistoryActivity::class.java
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
                getString(R.string.title_debug_navigation_kotlin),
                getString(R.string.description_debug_navigation_kotlin),
                DebugMapboxNavigationKt::class.java
            )
        )
    }
}
