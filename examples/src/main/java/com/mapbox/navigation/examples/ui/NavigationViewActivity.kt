package com.mapbox.navigation.examples.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.android.synthetic.main.activity_navigation_view.*

/**
 * This activity shows how to use a [com.mapbox.navigation.ui.NavigationView]
 * to implement a basic turn-by-turn navigation experience.
 */
class NavigationViewActivity : AppCompatActivity(), OnNavigationReadyCallback,
        NavigationListener,
        BannerInstructionsListener {

    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val route by lazy { getDirectionsRoute() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_view)

        navigationView.onCreate(savedInstanceState)
        navigationView.initialize(this, getInitialCameraPosition())
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        navigationView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationView.onResume()
    }

    override fun onStop() {
        super.onStop()
        navigationView.onStop()
    }

    override fun onPause() {
        super.onPause()
        navigationView.onPause()
    }

    override fun onDestroy() {
        navigationView.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navigationView.onRestoreInstanceState(savedInstanceState)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            ifNonNull(navigationView.retrieveNavigationMapboxMap()) { navMapboxMap ->
                this.navigationMapboxMap = navMapboxMap
                this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
                navigationView.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it }

                val optionsBuilder = NavigationViewOptions.builder()
                optionsBuilder.navigationListener(this)
                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)
                optionsBuilder.bannerInstructionsListener(this)
                optionsBuilder.navigationOptions(NavigationOptions.Builder().build())
                navigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions {
        return instructions!!
    }

    override fun onNavigationRunning() {
        // todo
    }

    override fun onNavigationFinished() {
        finish()
    }

    override fun onCancelNavigation() {
        navigationView.stopNavigation()
        finish()
    }

    private fun getInitialCameraPosition(): CameraPosition {
        val originCoordinate = route.routeOptions()?.coordinates()?.get(0)
        return CameraPosition.Builder()
                .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
                .zoom(15.0)
                .build()
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val directionsRouteAsJson = """
            {"routeIndex":"0","distance":2242.4,"duration":448.2,"geometry":"_hsagA~bomhFv@eArJoM|F_IlRgWvC_ExCtEvLjPfSrXbCdDeCdDmI~KkLzOyGbJqCzD}B`Bc\\xc@wBvES^Sb@cAtByApCu@v@_An@wGx@aSdCkCZZxE`Cn^x@zLt@bLF|@XdEXlEx@dMvDbn@TrDXvElClb@`@|G^|ElAlR|@~Mf@|H`@bGfHjgAv@vMX|EtAlRxA`UnCdb@jAvQdJtvAZvETlDXjE~Cdf@nEfr@JxAwDb@_o@pHgD`@[DYB{DnAgWxCmM?iH`A_AfB_@b@B\\ZpE|@`MlIhqAxE|t@JrAlAnRjBlYbKr~A\\~Erq@eIjFo@`@EvC]j@IfW{C","weight":753.0,"weight_name":"routability","legs":[{"distance":2242.4,"duration":448.2,"summary":"Pine Street, Sacramento Street","steps":[{"distance":113.9,"duration":28.3,"geometry":"_hsagA~bomhFv@eArJoM|F_IlRgWvC_E","name":"Beale Street","mode":"driving","maneuver":{"location":[-122.396736,37.791888],"bearing_before":0.0,"bearing_after":135.0,"instruction":"Head southeast on Beale Street","type":"depart","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":113.9,"announcement":"Head southeast on Beale Street, then turn right onto Mission Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eHead southeast on Beale Street, then turn right onto Mission Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":60.4,"announcement":"Turn right onto Mission Street, then turn right onto Fremont Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn right onto Mission Street, then turn right onto Fremont Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":113.9,"primary":{"text":"Mission Street","components":[{"text":"Mission Street","type":"text","abbr":"Mission St","abbr_priority":0}],"type":"turn","modifier":"right"}},{"distanceAlongGeometry":60.4,"primary":{"text":"Mission Street","components":[{"text":"Mission Street","type":"text","abbr":"Mission St","abbr_priority":0}],"type":"turn","modifier":"right"},"sub":{"text":"Fremont Street","components":[{"text":"Fremont Street","type":"text","abbr":"Fremont St","abbr_priority":0}],"type":"turn","modifier":"right"}}],"driving_side":"right","weight":52.9,"intersections":[{"location":[-122.396736,37.791888],"bearings":[135],"entry":[true],"out":0}]},{"distance":108.6,"duration":30.2,"geometry":"ozqagA`jmmhFxCtEvLjPfSrXbCdD","name":"Mission Street","mode":"driving","maneuver":{"location":[-122.395825,37.79116],"bearing_before":135.0,"bearing_after":225.0,"instruction":"Turn right onto Mission Street","type":"turn","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":53.9,"announcement":"Turn right onto Fremont Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn right onto Fremont Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":108.6,"primary":{"text":"Fremont Street","components":[{"text":"Fremont Street","type":"text","abbr":"Fremont St","abbr_priority":0}],"type":"turn","modifier":"right"}}],"driving_side":"right","weight":55.0,"intersections":[{"location":[-122.395825,37.79116],"bearings":[45,135,225,315],"entry":[true,true,true,false],"in":3,"out":2}]},{"distance":283.1,"duration":48.2,"geometry":"qopagA|`omhFeCdDmI~KkLzOyGbJqCzD}B`Bc\\xc@wBvES^Sb@cAtByApCu@v@_An@wGx@aSdCkCZ","name":"Fremont Street","mode":"driving","maneuver":{"location":[-122.396703,37.790473],"bearing_before":223.0,"bearing_after":315.0,"instruction":"Turn right onto Fremont Street","type":"turn","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":263.1,"announcement":"In 300 meters, turn left onto Pine Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eIn 300 meters, turn left onto Pine Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":88.1,"announcement":"Turn left onto Pine Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn left onto Pine Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":283.1,"primary":{"text":"Pine Street","components":[{"text":"Pine Street","type":"text","abbr":"Pine St","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":104.7,"intersections":[{"location":[-122.396703,37.790473],"bearings":[45,135,225,315],"entry":[false,false,true,true],"in":0,"out":3},{"location":[-122.398298,37.791734],"bearings":[135,300],"entry":[false,true],"in":0,"out":1}]},{"distance":920.7,"duration":176.9,"geometry":"yhtagAbxrmhFZxE`Cn^x@zLt@bLF|@XdEXlEx@dMvDbn@TrDXvElClb@`@|G^|ElAlR|@~Mf@|H`@bGfHjgAv@vMX|EtAlRxA`UnCdb@jAvQdJtvAZvETlDXjE~Cdf@nEfr@JxA","name":"Pine Street","mode":"driving","maneuver":{"location":[-122.39861,37.792413],"bearing_before":350.0,"bearing_after":260.0,"instruction":"Turn left onto Pine Street","type":"turn","modifier":"left"},"voiceInstructions":[{"distanceAlongGeometry":900.7,"announcement":"Continue on Pine Street for 900 meters","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eContinue on Pine Street for 900 meters\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":364.3,"announcement":"In 400 meters, turn right onto Powell Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eIn 400 meters, turn right onto Powell Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":78.1,"announcement":"Turn right onto Powell Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn right onto Powell Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":920.7,"primary":{"text":"Powell Street","components":[{"text":"Powell Street","type":"text","abbr":"Powell St","abbr_priority":0}],"type":"turn","modifier":"right"}}],"driving_side":"right","weight":274.5,"intersections":[{"location":[-122.39861,37.792413],"bearings":[75,165,255,345],"entry":[false,false,true,true],"in":1,"out":2},{"location":[-122.399785,37.792261],"bearings":[75,165,255,345],"entry":[false,true,true,false],"in":0,"out":2},{"location":[-122.400959,37.792116],"bearings":[75,165,255,345],"entry":[false,false,true,true],"in":0,"out":2},{"location":[-122.402598,37.791909],"bearings":[75,165,255,345],"entry":[false,true,true,false],"in":0,"out":2},{"location":[-122.404233,37.791703],"bearings":[75,180,255,345],"entry":[false,false,true,true],"in":0,"out":2},{"location":[-122.40576,37.791505],"bearings":[75,165,255,345],"entry":[false,false,true,true],"in":0,"out":2},{"location":[-122.407358,37.791301],"bearings":[75,165,255,345],"entry":[false,true,true,true],"in":0,"out":2}]},{"distance":214.7,"duration":45.400000000000006,"geometry":"svqagAn~fnhFwDb@_o@pHgD`@[DYB{DnAgWxCmM?iH`A_AfB_@b@","name":"Powell Street","mode":"driving","maneuver":{"location":[-122.408952,37.791098],"bearing_before":260.0,"bearing_after":350.0,"instruction":"Turn right onto Powell Street","type":"turn","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":194.7,"announcement":"In 200 meters, turn left onto Sacramento Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eIn 200 meters, turn left onto Sacramento Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":70.9,"announcement":"Turn left onto Sacramento Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn left onto Sacramento Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":214.7,"primary":{"text":"Sacramento Street","components":[{"text":"Sacramento Street","type":"text","abbr":"Sacramento St","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":102.4,"intersections":[{"location":[-122.408952,37.791098],"bearings":[75,165,255,345],"entry":[false,false,true,true],"in":0,"out":3},{"location":[-122.409143,37.792056],"bearings":[75,165,255,345],"entry":[true,false,false,true],"in":1,"out":3}]},{"distance":440.2,"duration":81.8,"geometry":"eluagAhxgnhFB\\ZpE|@`MlIhqAxE|t@JrAlAnRjBlYbKr~A\\~E","name":"Sacramento Street","mode":"driving","maneuver":{"location":[-122.409365,37.792979],"bearing_before":350.0,"bearing_after":260.0,"instruction":"Turn left onto Sacramento Street","type":"turn","modifier":"left"},"voiceInstructions":[{"distanceAlongGeometry":420.2,"announcement":"In 400 meters, turn left onto Jones Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eIn 400 meters, turn left onto Jones Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":80.7,"announcement":"Turn left onto Jones Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn left onto Jones Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":440.2,"primary":{"text":"Jones Street","components":[{"text":"Jones Street","type":"text","abbr":"Jones St","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":114.1,"intersections":[{"location":[-122.409365,37.792979],"bearings":[75,165,255,345],"entry":[false,true,true,true],"in":1,"out":2},{"location":[-122.411027,37.792765],"bearings":[75,165,255,345],"entry":[false,true,true,true],"in":0,"out":2},{"location":[-122.411932,37.79265],"bearings":[75,165,255,345],"entry":[false,true,true,false],"in":0,"out":2},{"location":[-122.412667,37.792557],"bearings":[75,165,255,345],"entry":[false,true,true,true],"in":0,"out":2}]},{"distance":161.2,"duration":37.4,"geometry":"wdtagAhmqnhFrq@eIjFo@`@EvC]j@IfW{C","name":"Jones Street","mode":"driving","maneuver":{"location":[-122.414309,37.792348],"bearing_before":260.0,"bearing_after":170.0,"instruction":"Turn left onto Jones Street","type":"turn","modifier":"left"},"voiceInstructions":[{"distanceAlongGeometry":141.2,"announcement":"In 100 meters, you will arrive at your destination","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eIn 100 meters, you will arrive at your destination\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":21.6,"announcement":"You have arrived at your destination","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eYou have arrived at your destination\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":161.2,"primary":{"text":"You will arrive","components":[{"text":"You will arrive","type":"text"}],"type":"arrive","modifier":"straight"}},{"distanceAlongGeometry":21.6,"primary":{"text":"You have arrived","components":[{"text":"You have arrived","type":"text"}],"type":"arrive","modifier":"straight"}}],"driving_side":"right","weight":49.4,"intersections":[{"location":[-122.414309,37.792348],"bearings":[75,165,255,345],"entry":[false,true,true,true],"in":0,"out":1},{"location":[-122.414122,37.79142],"bearings":[75,165,255,345],"entry":[true,true,true,false],"in":3,"out":1}]},{"distance":0.0,"duration":0.0,"geometry":"ikqagAh{pnhF","name":"Jones Street","mode":"driving","maneuver":{"location":[-122.414021,37.790917],"bearing_before":171.0,"bearing_after":0.0,"instruction":"You have arrived at your destination","type":"arrive"},"voiceInstructions":[],"bannerInstructions":[],"driving_side":"right","weight":0.0,"intersections":[{"location":[-122.414021,37.790917],"bearings":[351],"entry":[true],"in":0}]}],"annotation":{"distance":[4.377558046781132,29.048645741202307,19.932752580690952,48.576014751952926,11.943930596414795,12.720147968330114,34.58112560559672,50.96480933738249,10.349556668565532,10.428744124966986,26.062939337835754,33.61207745353073,22.152629772397148,11.584122498337331,8.22504067141732,73.25791925952063,11.603839142628226,1.7930152150745173,1.9339724019326627,6.4182775391464615,8.137686480966448,3.8827293592940286,4.137411880032337,15.778927074065876,36.18605026816596,7.882482295429149,9.70628239052936,44.88520535070527,19.777529614845466,18.70069668033605,2.7608352091952852,8.820943994209998,9.167951891088123,20.211271187411434,67.05841934336348,8.004641462478803,9.602196312248573,50.45852431578543,12.710488322858675,9.917381766860117,27.67756036992424,21.374879966188537,14.151341864692084,11.581827207587462,103.10585401718104,20.975906679342675,9.863007930072529,27.750876716852083,31.428390784757813,50.12927675688965,26.70541846540595,124.91524890087112,9.619673955896003,7.744241825057303,9.081282300519934,55.824774196397755,72.99751351106848,4.01125146327413,10.354407638921565,86.47389715542175,9.46174014762768,1.579336360053304,1.456588625710653,11.030579241536637,43.68327188987056,25.693275300415763,16.824625284570534,5.792905661239118,2.381194445403029,1.337050795424019,9.359356548680744,20.074574908738008,117.23790846433582,76.81587623559835,3.7514168675917774,27.764134475011225,37.66153518871731,136.19947456061075,9.984630741768928,91.22535667168732,13.293150952776184,1.90914496306627,8.555399787319097,2.4861312264216893,43.69699404532718],"congestion":["moderate","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","low","moderate","low","low","low","low","low","moderate","moderate","low","low","moderate","moderate","moderate","moderate","low","low","low","low","low","low","low","moderate","moderate","low","low","low","low","low","low","moderate","low","low","low","low","low","low","low","low","low","low","moderate","low","low","low","low","low","low","low","low","low","low","low"]}}],"routeOptions":{"baseUrl":"https://api.mapbox.com","user":"mapbox","profile":"driving-traffic","coordinates":[[-122.396796,37.7918406],[-122.4140199,37.7909168]],"alternatives":true,"language":"en","continue_straight":false,"roundabout_exits":false,"geometries":"polyline6","overview":"full","steps":true,"annotations":"congestion,distance","voice_instructions":true,"banner_instructions":true,"voice_units":"metric","access_token":"pk.eyJ1IjoianVuZGFpIiwiYSI6ImNrNW9qb3ZrdDA3bm0zZXBjc3E5Njdld2gifQ.e8XLy4jnrnpWnGKGiEcVMQ","uuid":"ckblcybpcnz7379uen64vrcuj"},"voiceLocale":"en-US"}
        """.trimIndent()

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }
}
