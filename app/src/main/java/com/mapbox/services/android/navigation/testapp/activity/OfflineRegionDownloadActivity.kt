package com.mapbox.services.android.navigation.testapp.activity

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import com.mapbox.api.routetiles.v1.versions.models.RouteTileVersionsResponse
import com.mapbox.geojson.BoundingBox
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.v5.navigation.OfflineTileVersions
import com.mapbox.services.android.navigation.v5.navigation.OfflineTiles
import com.mapbox.services.android.navigation.v5.navigation.RoutingTileDownloadManager
import kotlinx.android.synthetic.main.activity_offline_region_download.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OfflineRegionDownloadActivity : AppCompatActivity(), RoutingTileDownloadManager.RoutingTileDownloadListener {

    lateinit var mapboxMap: MapboxMap
    private val disabledGrey by lazy { resources.getColor(R.color.md_grey_700) }
    private val enabledBlue by lazy { resources.getColor(R.color.mapbox_blue) }
    private var downloadButtonEnabled: Boolean = false

    private val boundingBox: BoundingBox
        get() {
            val top = selectionBox.top - mapView.top
            val left = selectionBox.left - mapView.left
            val right = left + selectionBox.width
            val bottom = top + selectionBox.height

            val southWest = mapboxMap.projection.fromScreenLocation(
                    PointF(left.toFloat(), bottom.toFloat()))
            val northEast = mapboxMap.projection.fromScreenLocation(
                    PointF(right.toFloat(), top.toFloat()))

            return BoundingBox.fromLngLats(
                    southWest.longitude, southWest.latitude,
                    northEast.longitude, northEast.latitude)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_region_download)

        downloadButton.setOnClickListener { onDownloadClick() }
        setupSpinner()
        setupMapView(savedInstanceState)
    }

    fun setupSpinner() {
        OfflineTileVersions(Mapbox.getAccessToken())
                .getRouteTileVersions(object : Callback<RouteTileVersionsResponse> {
                    override fun onResponse(call: Call<RouteTileVersionsResponse>, response:
                    Response<RouteTileVersionsResponse>) {
                        response.body().let {
                            if (it != null) setupSpinner(it.availableVersions()) else onVersionFetchFailed()
                        }
                    }

                    override fun onFailure(call: Call<RouteTileVersionsResponse>, throwable: Throwable) {
                        onVersionFetchFailed()
                    }
                })
    }
    fun onVersionFetchFailed() {
        showToast("Unable to fetch versions")
        setDownloadEnabled(false)
        downloadButton.visibility = GONE
        restartVersionFetchButton.visibility = VISIBLE
        restartVersionFetchButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                setupSpinner()
                restartVersionFetchButton.setOnClickListener(null)
            }

        })
    }

    fun setupSpinner(versions: MutableList<String>) {
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, versions)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        versionSpinner.adapter = arrayAdapter
        versionSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setDownloadEnabled(true, "Download Region")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setDownloadEnabled(false, "Download Region")
            }
        }

        versionSpinner.visibility = VISIBLE
    }

    private fun setupMapView(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            addBoundingBoxToMap()
        }
    }

    private fun addBoundingBoxToMap() {
        mapboxMap.apply {
            addSource(GeoJsonSource("bounding-box-source"))
            addLayer(FillLayer("bounding-box-layer", "bounding-box-source")
                    .withProperties(fillColor(Color.parseColor("#50667F"))))
        }
    }

    private fun onDownloadClick() {
        // todo check that download is less than 1.5 million square kilometers
        if (!downloadButtonEnabled) {
            return
        }

        setDownloadEnabled(false, "Requesting tiles....")
        val builder = OfflineTiles.builder()
                .accessToken(Mapbox.getAccessToken())
                .version(versionSpinner.selectedItem as String)
                .boundingBox(boundingBox)

        val routingTileDownloadManager = RoutingTileDownloadManager()
        routingTileDownloadManager.setListener(this)
        routingTileDownloadManager.startDownload(builder.build())
    }

    private fun setDownloadEnabled(enabled: Boolean) {
        setDownloadEnabled(enabled, "Download Region")

    }

    private fun setDownloadEnabled(enabled: Boolean, text: String) {
        downloadButtonEnabled = enabled
        versionSpinner.isEnabled = enabled
        loading.visibility = if (enabled) View.GONE else View.VISIBLE
        downloadButton.setBackgroundColor(if (enabled) enabledBlue else disabledGrey)
        downloadButton.text = text
    }

    /*
   * Download listeners
   */

    override fun onError(throwable: Throwable) {
        setDownloadEnabled(true, "Download Region")
        showToast("There was an error with the download. Please try again.")
    }

    override fun onProgressUpdate(percent: Int) {
        setDownloadEnabled(false, percent.toString() + "%...")

    }

    override fun onCompletion(successful: Boolean) {
        setDownloadEnabled(true, "Download Region")
        showToast(if (successful) "Download complete" else "Download cancelled")
    }

    /*
   * Basic mapView boilerplate
   */

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun showToast(str: String) {
        Toast.makeText(applicationContext, str,
                Toast.LENGTH_LONG).show()
    }
}
