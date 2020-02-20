package com.mapbox.navigation.examples

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.mapbox.navigation.examples.core.OffboardRouterActivityJava
import com.mapbox.navigation.examples.core.OffboardRouterActivityKt
import com.mapbox.navigation.examples.core.OnboardRouterActivityJava
import com.mapbox.navigation.examples.core.OnboardRouterActivityKt
import com.mapbox.navigation.examples.core.SimpleMapboxNavigationKt
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
                getString(R.string.title_trip_service_kotlin),
                getString(R.string.description_trip_service_kotlin),
                TripServiceActivityKt::class.java
            ),
            SampleItem(
                getString(R.string.title_trip_session_kotlin),
                getString(R.string.description_trip_session_kotlin),
                TripSessionActivityKt::class.java
            )
        )
    }
}
