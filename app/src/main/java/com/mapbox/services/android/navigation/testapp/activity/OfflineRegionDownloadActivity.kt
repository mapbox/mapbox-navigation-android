package com.mapbox.services.android.navigation.testapp.activity

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import com.mapbox.geojson.BoundingBox
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.v5.navigation.*
import kotlinx.android.synthetic.main.activity_offline_region_download.*
import org.json.JSONObject
import timber.log.Timber

class OfflineRegionDownloadActivity : AppCompatActivity(), RouteTileDownloadListener, OnOfflineTilesRemovedCallback {
    lateinit var mapboxMap: MapboxMap
    private val EXTERNAL_STORAGE_PERMISSION = 1
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
    private val mapboxOfflineRouter: MapboxOfflineRouter
        get() {
            return MapboxOfflineRouter(obtainOfflineDirectory())
        }
    private lateinit var offlineManager: OfflineManager
    private var offlineRegion: OfflineRegion? = null
    private val offlineRegionCallback = object : OfflineManager.CreateOfflineRegionCallback {
        override fun onCreate(offlineRegion: OfflineRegion?) {
            Timber.d("Offline region created: %s", "NavigationOfflineMapsRegion")
            this@OfflineRegionDownloadActivity.offlineRegion = offlineRegion
            launchMapsDownload()
        }

        override fun onError(error: String?) {
            Timber.e("Error: %s", error)
        }
    }

    private val offlineRegionObserver = object : OfflineRegion.OfflineRegionObserver {
        override fun mapboxTileCountLimitExceeded(limit: Long) {
            Timber.e("Mapbox tile count limit exceeded: %s", limit)
        }

        override fun onStatusChanged(status: OfflineRegionStatus?) {
            Timber.d("%s/%s resources; %s bytes downloaded.",
                    status?.completedResourceCount,
                    status?.requiredResourceCount,
                    status?.completedResourceSize)
            if (status?.isComplete!!) {
                downloadSelectedRegion()
            }
        }

        override fun onError(error: OfflineRegionError?) {
            Timber.e("onError reason: %s", error?.reason)
            Timber.e("onError message: %s", error?.message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_region_download)

        downloadButton.setOnClickListener { onDownloadClick() }
        removeButton.setOnClickListener { onRemoveClick() }
        setupSpinner()
        setupMapView(savedInstanceState)
    }

    fun setupSpinner() {
        mapboxOfflineRouter
                .fetchAvailableTileVersions(Mapbox.getAccessToken(),
                        object : OnTileVersionsFoundCallback {
                            override fun onVersionsFound(availableVersions: MutableList<String>) {
                                setupSpinner(availableVersions)
                            }

                            override fun onError(error: OfflineError) {
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
            mapboxMap.setStyle(Style.LIGHT) {
                it.addSource(GeoJsonSource("bounding-box-source"))
                it.addLayer(FillLayer("bounding-box-layer", "bounding-box-source")
                        .withProperties(fillColor(Color.parseColor("#50667F"))))
                offlineManager = OfflineManager.getInstance(this)
            }
            this.mapboxMap = mapboxMap
            mapboxMap.uiSettings.isRotateGesturesEnabled = false
        }
    }

    private fun onDownloadClick() {
        // todo check that download is less than 1.5 million square kilometers
        if (!downloadButtonEnabled) {
            return
        }

        if (ContextCompat.checkSelfPermission(
                        this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 1)
        } else {
            downloadMapsRegion()
        }
    }

    private fun onRemoveClick() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            showToast("Storage permissions should be granted. Please try again.")
        } else {
            removeSelectedRegion()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            EXTERNAL_STORAGE_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED)) {
                    downloadMapsRegion()
                } else {
                    setDownloadButtonEnabled(false)
                }
            }
        }
    }

    private fun downloadMapsRegion() {
        showDownloading(false, "Requesting tiles....")
        // TODO Hardcoding OfflineRegionDefinitionProvider values for testing / debugging purposes
        val styleUrl: String? = "mapbox://styles/mapbox/navigation-guidance-day-v4"
        //val styleUrl: String? = mapboxMap.style?.url
        val bounds: LatLngBounds = LatLngBounds.from(boundingBox.north(), boundingBox.east(), boundingBox.south(), boundingBox.west())
        // TODO Hardcoding OfflineRegionDefinitionProvider values for testing / debugging purposes
        val minZoom: Double = 11.0
        val maxZoom: Double = 17.0
        //val minZoom: Double = mapboxMap.cameraPosition.zoom
        //val maxZoom: Double = mapboxMap.maxZoomLevel
        val pixelRatio: Float = this.resources.displayMetrics.density
        val definition: OfflineTilePyramidRegionDefinition = OfflineTilePyramidRegionDefinition(
                styleUrl, bounds, minZoom, maxZoom, pixelRatio)

        val metadata: ByteArray
        val jsonObject: JSONObject = JSONObject()
        jsonObject.put("FIELD_REGION_NAME", "NavigationOfflineMapsRegion")
        val json: String = jsonObject.toString()
        metadata = json.toByteArray()
        offlineManager.createOfflineRegion(definition, metadata, offlineRegionCallback)
    }

    private fun launchMapsDownload() {
        offlineRegion?.setObserver(offlineRegionObserver)
        offlineRegion?.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun downloadSelectedRegion() {
        val builder = OfflineTiles.builder()
                .accessToken(Mapbox.getAccessToken())
                .version(versionSpinner.selectedItem as String)
                .boundingBox(boundingBox)

        mapboxOfflineRouter.downloadTiles(builder.build(), this)
    }

    private fun removeSelectedRegion() {
        showRemoving(true, "Removing tiles....")
        val version: String = retrieveOfflineVersionFromPreferences()
        mapboxOfflineRouter.removeTiles(version, boundingBox, this)
    }

    private fun retrieveOfflineVersionFromPreferences(): String {
        val context = application
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.offline_version_key), "")
    }

    private fun obtainOfflineDirectory(): String {
        val offline = Environment.getExternalStoragePublicDirectory("Offline")
        if (!offline.exists()) {
            Timber.d("Offline directory does not exist")
            offline.mkdirs()
        }
        return offline.absolutePath
    }

    private fun showDownloading(downloading: Boolean, message: String) {
        versionSpinner.isEnabled = !downloading
        loading.visibility = if (downloading) View.VISIBLE else View.GONE
        setDownloadButtonEnabled(!downloading, message)
    }

    private fun showRemoving(removing: Boolean, message: String) {
        versionSpinner.isEnabled = !removing
        loading.visibility = if (removing) View.VISIBLE else View.GONE
        updateRemoveButton(!removing, message)
    }

    private fun setDownloadButtonEnabled(enabled: Boolean) {
        setDownloadButtonEnabled(enabled, "Download Region")
    }

    private fun setDownloadButtonEnabled(enabled: Boolean, text: String) {
        downloadButtonEnabled = enabled

        downloadButton.setBackgroundColor(if (enabled) enabledBlue else disabledGrey)
        downloadButton.text = text
    }

    private fun updateRemoveButton(enabled: Boolean, text: String) {
        removeButton.text = text
        removeButton.isEnabled = enabled
    }

    /*
   * Download listeners
   */

    override fun onError(error: OfflineError) {
        setDownloadButtonEnabled(true)
        showToast("There was an error with the download. Please try again.")
    }

    override fun onProgressUpdate(percent: Int) {
        showDownloading(false, percent.toString() + "%...")
    }

    override fun onCompletion() {
        setDownloadButtonEnabled(true)
        showToast("Download complete")
    }

    override fun onRemoved(numberOfTiles: Long) {
        showRemoving(false, "Remove Region")
        showToast("$numberOfTiles routing tiles were removed")
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
        offlineRegion?.setObserver(null)
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
