package com.mapbox.services.android.navigation.testapp.activity

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.support.annotation.RequiresPermission
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
        setDownloadButtonEnabled(false)
        versionSpinnerContainer.visibility = GONE
        restartVersionFetchButton.visibility = VISIBLE
        restartVersionFetchButton.setOnClickListener {
            setupSpinner()
            restartVersionFetchButton.setOnClickListener(null)
        }
    }


    fun setupSpinner(versions: MutableList<String>) {
        restartVersionFetchButton.visibility = GONE
        versionSpinnerContainer.visibility = VISIBLE

        ArrayAdapter(this, android.R.layout.simple_spinner_item, versions)
                .also { arrayAdapter ->
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    versionSpinner.adapter = arrayAdapter
                }

        versionSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                setDownloadButtonEnabled(false)
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setDownloadButtonEnabled(position != 0)

                versionSpinner.selectedItem.run {
                    setDownloadButtonEnabled((this as String).isNotEmpty())
                }
            }
        }
    }

    private fun setupMapView(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.uiSettings.isRotateGesturesEnabled = false
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

    @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun onDownloadClick() {
        // todo check that download is less than 1.5 million square kilometers
        if (!downloadButtonEnabled) {
            return
        }

        showDownloading(false, "Requesting tiles....")
        val builder = OfflineTiles.builder()
                .accessToken(Mapbox.getAccessToken())
                .version(versionSpinner.selectedItem as String)
                .boundingBox(boundingBox)

        RoutingTileDownloadManager().let {

            it.setListener(this)
            var offlineTiles = builder.build()
            it.startDownload(offlineTiles)
        }
    }

    private fun showDownloading(downloading: Boolean, message: String) {
        versionSpinner.isEnabled = !downloading
        loading.visibility = if (downloading) View.VISIBLE else View.GONE
        setDownloadButtonEnabled(!downloading, message)
    }

    private fun setDownloadButtonEnabled(enabled: Boolean) {
        setDownloadButtonEnabled(enabled, "Download Region")
    }

    private fun setDownloadButtonEnabled(enabled: Boolean, text: String) {
        downloadButtonEnabled = enabled

        downloadButton.setBackgroundColor(if (enabled) enabledBlue else disabledGrey)
        downloadButton.text = text
    }

    /*
   * Download listeners
   */

    override fun onError(throwable: Throwable) {
        setDownloadButtonEnabled(true)
        showToast("There was an error with the download. Please try again.")
    }

    override fun onProgressUpdate(percent: Int) {
        showDownloading(false, percent.toString() + "%...")

    }

    override fun onCompletion(successful: Boolean) {
        setDownloadButtonEnabled(true)
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
                Toast.LENGTH_SHORT).show()
    }
}
