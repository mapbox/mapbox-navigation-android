package com.mapbox.navigation.examples.settings

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.gson.GeometryGeoJson
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineRegionError
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.services.android.navigation.v5.navigation.MapboxOfflineRouter
import com.mapbox.services.android.navigation.v5.navigation.OfflineError
import com.mapbox.services.android.navigation.v5.navigation.OfflineTiles
import com.mapbox.services.android.navigation.v5.navigation.OnOfflineTilesRemovedCallback
import com.mapbox.services.android.navigation.v5.navigation.OnTileVersionsFoundCallback
import com.mapbox.services.android.navigation.v5.navigation.RouteTileDownloadListener
import kotlinx.android.synthetic.main.activity_offline_region_download.*
import org.json.JSONObject

class OfflineRegionDownloadActivity : AppCompatActivity(), RouteTileDownloadListener,
    OnOfflineTilesRemovedCallback {

    companion object {
        private const val EXTERNAL_STORAGE_PERMISSION = 1
    }

    private lateinit var mapboxMap: MapboxMap
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
                PointF(left.toFloat(), bottom.toFloat())
            )
            val northEast = mapboxMap.projection.fromScreenLocation(
                PointF(right.toFloat(), top.toFloat())
            )

            return BoundingBox.fromLngLats(
                southWest.longitude, southWest.latitude,
                northEast.longitude, northEast.latitude
            )
        }
    private val mapboxOfflineRouter: MapboxOfflineRouter
        get() {
            return MapboxOfflineRouter(obtainOfflineDirectory())
        }

    private lateinit var offlineManager: OfflineManager
    private var offlineRegion: OfflineRegion? = null
    private val offlineRegionCallback = object : OfflineManager.CreateOfflineRegionCallback {
        override fun onCreate(offlineRegion: OfflineRegion?) {
            MapboxLogger.d(Message("Offline region created: NavigationOfflineMapsRegion"))
            this@OfflineRegionDownloadActivity.offlineRegion = offlineRegion
            launchMapsDownload()
        }

        override fun onError(error: String?) {
            MapboxLogger.e(Message("Error: $error"))
        }
    }

    private var isDownloadCompleted: Boolean = false
    private val offlineRegionObserver = object : OfflineRegion.OfflineRegionObserver {
        override fun mapboxTileCountLimitExceeded(limit: Long) {
            MapboxLogger.e(Message("Mapbox tile count limit exceeded: $limit"))
        }

        override fun onStatusChanged(offlineRegionStatus: OfflineRegionStatus?) {
            offlineRegionStatus?.let { status ->
                MapboxLogger.d(Message("${status.completedResourceCount}/${status.requiredResourceCount} resources; " +
                    "${status.completedResourceSize} bytes downloaded."))
                if (status.isComplete && !isDownloadCompleted) {
                    isDownloadCompleted = true
                    downloadSelectedRegion()
                }
            }
        }

        override fun onError(error: OfflineRegionError?) {
            MapboxLogger.e(Message("onError reason: ${error?.reason}"))
            MapboxLogger.e(Message("onError reason: ${error?.message}"))
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

    private fun setupSpinner() {
        val token = Mapbox.getAccessToken() ?: return
        mapboxOfflineRouter
            .fetchAvailableTileVersions(token,
                object : OnTileVersionsFoundCallback {
                    override fun onVersionsFound(availableVersions: List<String>) {
                        setupSpinner(availableVersions)
                    }

                    override fun onError(error: OfflineError) {
                        onVersionFetchFailed()
                    }
                }
            )
    }

    fun onVersionFetchFailed() {
        showToast("Unable to fetch versions")
        setDownloadButtonEnabled(false)
        versionSpinnerContainer.visibility = View.GONE
        restartVersionFetchButton.visibility = View.VISIBLE
        restartVersionFetchButton.setOnClickListener {
            setupSpinner()
            restartVersionFetchButton.setOnClickListener(null)
        }
    }

    fun setupSpinner(versions: List<String>) {
        restartVersionFetchButton.visibility = View.GONE
        versionSpinnerContainer.visibility = View.VISIBLE

        ArrayAdapter(this, android.R.layout.simple_spinner_item, versions)
            .also { arrayAdapter ->
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                versionSpinner.adapter = arrayAdapter
            }

        versionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                setDownloadButtonEnabled(false)
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
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
                it.addLayer(
                    FillLayer("bounding-box-layer", "bounding-box-source")
                        .withProperties(PropertyFactory.fillColor(Color.parseColor("#50667F")))
                )
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
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        } else {
            downloadMapsRegion()
        }
    }

    private fun onRemoveClick() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("Storage permissions should be granted. Please try again.")
        } else {
            removeSelectedRegion()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            EXTERNAL_STORAGE_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
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
        // val styleUrl: String? = mapboxMap.style?.url
        val bounds: LatLngBounds = LatLngBounds.from(
            boundingBox.north(),
            boundingBox.east(),
            boundingBox.south(),
            boundingBox.west()
        )
        // TODO Testing downloading a Geometry
        val geometry: Geometry = GeometryGeoJson.fromJson(
            "{\"type\":\"Polygon\",\"coordinates\":[[[-77.152533,39.085537],[-77.152533,39.083038],[-77.150031,39.083038],[-77.150031,39.085537],[-77.147529,39.085537],[-77.147529,39.088039],[-77.147529,39.090538],[-77.150031,39.090538],[-77.150031,39.093037],[-77.150031,39.095539],[-77.150031,39.098038],[-77.150031,39.100540],[-77.150031,39.103039],[-77.152533,39.103039],[-77.152533,39.105537],[-77.155028,39.105537],[-77.155028,39.108040],[-77.155028,39.110538],[-77.157531,39.110538],[-77.157531,39.113037],[-77.160033,39.113037],[-77.160033,39.115536],[-77.162528,39.115540],[-77.162528,39.118038],[-77.165030,39.118038],[-77.165030,39.115536],[-77.167533,39.115536],[-77.167533,39.113037],[-77.167533,39.110538],[-77.165030,39.110538],[-77.165030,39.108040],[-77.162536,39.108036],[-77.162536,39.105537],[-77.162536,39.103039],[-77.160033,39.103039],[-77.160033,39.100540],[-77.157531,39.100536],[-77.157531,39.098038],[-77.157531,39.095535],[-77.157531,39.093037],[-77.157531,39.090538],[-77.157531,39.088039],[-77.155036,39.088036],[-77.155036,39.085537],[-77.152533,39.085537]]]}"
        )
        // TODO Hardcoding OfflineRegionDefinitionProvider values for testing / debugging purposes
        val minZoom = 11.0
        val maxZoom = 17.0
        // val minZoom: Double = mapboxMap.cameraPosition.zoom
        // val maxZoom: Double = mapboxMap.maxZoomLevel
        val pixelRatio: Float = this.resources.displayMetrics.density
        val definition = OfflineTilePyramidRegionDefinition(styleUrl, bounds, minZoom, maxZoom, pixelRatio)
        // TODO Testing downloading a Geometry using OfflineGeometryRegionDefinition as definition
        // val definition: OfflineGeometryRegionDefinition = OfflineGeometryRegionDefinition(
        //        styleUrl, geometry, minZoom, maxZoom, pixelRatio)

        val metadata: ByteArray
        val jsonObject = JSONObject()
        jsonObject.put("FIELD_REGION_NAME", "NavigationOfflineMapsRegion")
        val json: String = jsonObject.toString()
        metadata = json.toByteArray()
        offlineManager.createOfflineRegion(definition, metadata, offlineRegionCallback)
    }

    private fun launchMapsDownload() {
        offlineRegion?.let { offlineRegion ->
            offlineRegion.setObserver(offlineRegionObserver)
            offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
        }
    }

    private fun downloadSelectedRegion() {
        val token = Mapbox.getAccessToken() ?: return

        val builder = OfflineTiles.builder(applicationContext)
            .accessToken(token)
            .version(versionSpinner.selectedItem as String)
            .boundingBox(boundingBox)

        mapboxOfflineRouter.downloadTiles(builder.build(), this)
    }

    private fun removeSelectedRegion() {
        showRemoving(true, "Removing tiles....")
        retrieveOfflineVersionFromPreferences()?.let { version ->
            mapboxOfflineRouter.removeTiles(version, boundingBox, this)
        }
    }

    private fun retrieveOfflineVersionFromPreferences(): String? {
        val context = application
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.offline_version_key), "")
    }

    private fun obtainOfflineDirectory(): String {
        val offline = Environment.getExternalStoragePublicDirectory("Offline")
        if (!offline.exists()) {
            MapboxLogger.d(Message("Offline directory does not exist"))
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
        isDownloadCompleted = false
        showToast("There was an error with the download: ${error.message}. Please try again.")
    }

    override fun onProgressUpdate(percent: Int) {
        showDownloading(false, "$percent%...")
    }

    override fun onCompletion() {
        setDownloadButtonEnabled(true)
        isDownloadCompleted = false
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
        Toast.makeText(
            applicationContext, str,
            Toast.LENGTH_SHORT
        ).show()
    }
}
