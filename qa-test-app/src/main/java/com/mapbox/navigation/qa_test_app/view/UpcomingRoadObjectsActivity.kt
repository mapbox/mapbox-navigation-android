package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.children
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutDrawerMenuNavViewBinding
import com.mapbox.navigation.qa_test_app.view.base.DrawerActivity
import com.mapbox.navigation.qa_test_app.view.customnavview.dp
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class UpcomingRoadObjectsActivity : DrawerActivity() {

    private lateinit var binding: LayoutActivityNavigationViewBinding
    private lateinit var menuBinding: LayoutDrawerMenuNavViewBinding

    override fun onCreateContentView(): View {
        binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreateMenuView(): View {
        menuBinding = LayoutDrawerMenuNavViewBinding.inflate(layoutInflater)
        return menuBinding.root
    }

    lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        menuBinding.toggleReplay.isChecked = binding.navigationView.api.isReplayEnabled()
        menuBinding.toggleReplay.setOnCheckedChangeListener { _, isChecked ->
            binding.navigationView.api.routeReplayEnabled(isChecked)
        }

        binding.navigationView.setRouteOptionsInterceptor {
            it.customizeOptions()
        }

        val ctx = this
        binding.navigationView.customizeViewBinders {
            customActionButtons = listOf(
                ActionButtonDescription(zoomToRomeButton())
            )
            infoPanelContentBinder = UIBinder {
                val rv = RecyclerView(ctx)
                rv.updatePadding(bottom = 50.dp)
                rv.layoutManager = LinearLayoutManager(ctx)
                it.removeAllViews()
                it.addView(
                    rv,
                    MarginLayoutParams(
                        MarginLayoutParams.MATCH_PARENT,
                        MarginLayoutParams.MATCH_PARENT,
                    )
                )
                UpcomingRoadObjectsListComponent(rv)
            }
        }
    }

    private fun zoomToRomeButton() = MapboxExtendableButton(this).also {
        it.iconImage.setImageDrawable(getDrawable(it.context, R.drawable.mapbox_ic_marker))
        it.setOnClickListener {
            findMapView()?.apply {
                camera.flyTo(
                    CameraOptions.Builder()
                        .center(ROME)
                        .zoom(12.0)
                        .build()
                )
            }
        }
    }

    private fun findMapView(): MapView? =
        binding.navigationView.findViewById<FrameLayout>(R.id.mapViewLayout)
            .children.firstOrNull() as? MapView

    private val ROME = Point.fromLngLat(
        12.483838237364807,
        41.88237804754755
    )

    private fun RouteOptions.Builder.customizeOptions() =
        profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .voiceInstructions(true)
            .bannerInstructions(true)
            .steps(true)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_CLOSURE,
                    DirectionsCriteria.ANNOTATION_CONGESTION,
                    DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                    DirectionsCriteria.ANNOTATION_DURATION,
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_DISTANCE,
                    DirectionsCriteria.ANNOTATION_MAXSPEED,
                )
            )
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .alternatives(true)
            .continueStraight(false)
            .roundaboutExits(true)

    private class UpcomingRoadObjectsListComponent(
        private val recyclerView: RecyclerView
    ) : UIComponent() {

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)

            val adapter = UpcomingRoadObjectsAdapter()
            recyclerView.adapter = adapter

            mapboxNavigation.flowRouteProgress().observe {
                adapter.roadObjects = it.upcomingRoadObjects
            }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            super.onDetached(mapboxNavigation)
            recyclerView.adapter = null
        }
    }

    class UpcomingRoadObjectsAdapter :
        RecyclerView.Adapter<UpcomingRoadObjectsAdapter.ViewHolder>() {

        var roadObjects: List<UpcomingRoadObject> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val indexText: TextView = view.findViewById(R.id.roadObjectIndex)
            private val typeText: TextView = view.findViewById(R.id.roadObjectType)
            private val distanceText: TextView = view.findViewById(R.id.roadObjectDistance)

            fun bind(index: Int, upcomingRoadObject: UpcomingRoadObject) {
                indexText.text = "#$index"
                typeText.text = when (upcomingRoadObject.roadObject.objectType) {
                    RoadObjectType.TUNNEL -> "TUNNEL"
                    RoadObjectType.COUNTRY_BORDER_CROSSING -> "COUNTRY_BORDER_CROSSING"
                    RoadObjectType.TOLL_COLLECTION -> "TOLL_COLLECTION"
                    RoadObjectType.REST_STOP -> "REST_STOP"
                    RoadObjectType.RESTRICTED_AREA -> "RESTRICTED_AREA"
                    RoadObjectType.BRIDGE -> "BRIDGE"
                    RoadObjectType.INCIDENT -> "INCIDENT"
                    RoadObjectType.CUSTOM -> "CUSTOM"
                    RoadObjectType.RAILWAY_CROSSING -> "RAILWAY_CROSSING"
                    else -> "???"
                }
                distanceText.text = upcomingRoadObject.distanceToStart?.let {
                    "%.2f meters".format(it)
                } ?: "???"
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_road_object, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.bind(position, roadObjects[position])
        }

        override fun getItemCount() = roadObjects.size
    }
}
