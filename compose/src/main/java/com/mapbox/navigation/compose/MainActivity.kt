package com.mapbox.navigation.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropValue
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.backgroundLayer
import com.mapbox.maps.plugin.logo.logo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DestinationCard()
//            MapboxNavigationTheme {
//            }
        }
    }
}

private const val LATITUDE = 60.239
private const val LONGITUDE = 25.004

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DestinationCard() {
    val scaffoldState = rememberBackdropScaffoldState(BackdropValue.Revealed)
    MapboxMap()
    BackdropScaffold(
        backLayerBackgroundColor = Color.Transparent,
        frontLayerScrimColor = Color.Transparent,
        scaffoldState = scaffoldState,
        headerHeight = 350.dp,
        appBar = {
            TopAppBar(
                title = { Text("Backdrop") },
                elevation = 0.dp,
                backgroundColor = Color.Transparent
            )
        },
        backLayerContent = {
//            MapboxMap()
            Text("lets go")
        },
        frontLayerContent = {
            MapboxMap()
        }
    )
}

@Composable
private fun MapboxMap() {
    AndroidView(
        factory = { context ->
            val mapView = MapView(context).apply {
                getMapboxMap().apply {
                    loadStyleUri(Style.MAPBOX_STREETS)
                    setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(LONGITUDE, LATITUDE))
                            .zoom(9.0)
                            .build()
                    )
                    this.getMapOptions().viewportMode
                }
            }
            mapView.logo.marginBottom = 100.0f
            mapView
        },
        modifier =  Modifier.fillMaxSize()
    )
}
