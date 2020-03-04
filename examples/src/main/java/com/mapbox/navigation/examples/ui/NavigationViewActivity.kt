package com.mapbox.navigation.examples.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.*
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_navigation_view.*
import timber.log.Timber

class NavigationViewActivity : AppCompatActivity(), OnNavigationReadyCallback, NavigationListener {

    private val directionsRouteAsJson = "{\"routeIndex\":\"0\",\"distance\":2793.6,\"duration\":788.3,\"geometry\":\"ypragA~enmhFpP}TvC_ExCtEvLjPfSrXbCdDpCxD`@h@bY~_@jDvErAhBlBlCvC|Dl^vf@^f@fKnNbf@lp@xPnUfCjDnCjDjAxAzJ`NfLtOxApBdCjD~_@ji@jUz[tIrLjCnDkCpDoIjLiJhM}TzZqNnQwAtA_An@gA^aALyBRoId@k@Bk@F}@F_Ef@gBVoDh@gCZaVrCcAJa\\\\xD_[rDoFn@gLtAoH|@xAnU`D~f@ZnErBt[pChb@bCz^hBtXLxB\\\\fFz@xMnHziANbCB`@Bb@VxDlAhRzDdm@zBv]tAjTbEfo@fCd`@pAbSlIpqAlAdRrIprAfArPxIjtAbAhOlEvq@nClb@}YlDcALc\\\\zDe\\\\|DfDfh@\",\"weight\":1144.6,\"weight_name\":\"routability\",\"legs\":[{\"distance\":2793.6,\"duration\":788.3,\"summary\":\"Mission Street, Sutter Street\",\"steps\":[{\"distance\":55.9,\"duration\":14.8,\"geometry\":\"ypragA~enmhFpP}TvC_E\",\"name\":\"Beale Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.396272,37.791517],\"bearing_before\":0.0,\"bearing_after\":135.0,\"instruction\":\"Head southeast on Beale Street\",\"type\":\"depart\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":55.9,\"announcement\":\"Head southeast on Beale Street, then turn right onto Mission Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eHead southeast on Beale Street, then turn right onto Mission Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":55.9,\"primary\":{\"text\":\"Mission Street\",\"components\":[{\"text\":\"Mission Street\",\"type\":\"text\",\"abbr\":\"Mission St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":39.4,\"intersections\":[{\"location\":[-122.396272,37.791517],\"bearings\":[135],\"entry\":[true],\"out\":0}]},{\"distance\":771.7,\"duration\":287.90000000000003,\"geometry\":\"ozqagA`jmmhFxCtEvLjPfSrXbCdDpCxD`@h@bY~_@jDvErAhBlBlCvC|Dl^vf@^f@fKnNbf@lp@xPnUfCjDnCjDjAxAzJ`NfLtOxApBdCjD~_@ji@jUz[tIrLjCnD\",\"name\":\"Mission Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.395825,37.79116],\"bearing_before\":135.0,\"bearing_after\":225.0,\"instruction\":\"Turn right onto Mission Street\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":751.7,\"announcement\":\"Continue on Mission Street for a half mile\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue on Mission Street for a half mile\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":187.6,\"announcement\":\"In 700 feet, turn right onto 3rd Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 700 feet, turn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3rd\\u003c/say-as\\u003e Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":40.2,\"announcement\":\"Turn right onto 3rd Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3rd\\u003c/say-as\\u003e Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":771.7,\"primary\":{\"text\":\"3rd Street\",\"components\":[{\"text\":\"3rd Street\",\"type\":\"text\",\"abbr\":\"3rd St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":360.6,\"intersections\":[{\"location\":[-122.395825,37.79116],\"bearings\":[45,135,225,315],\"entry\":[true,true,true,false],\"in\":3,\"out\":2},{\"location\":[-122.396703,37.790473],\"bearings\":[45,135,225,315],\"entry\":[false,false,true,true],\"in\":0,\"out\":2},{\"location\":[-122.397577,37.789782],\"bearings\":[45,135,225,315],\"entry\":[false,false,true,false],\"in\":0,\"out\":2},{\"location\":[-122.399813,37.788012],\"bearings\":[45,135,225,315],\"entry\":[false,false,true,true],\"in\":0,\"out\":2},{\"location\":[-122.400509,37.787455],\"bearings\":[45,135,225,315],\"entry\":[false,false,true,false],\"in\":0,\"out\":2}]},{\"distance\":448.7,\"duration\":121.7,\"geometry\":\"ihhagApnymhFkCpDoIjLiJhM}TzZqNnQwAtA_An@gA^aALyBRoId@k@Bk@F}@F_Ef@gBVoDh@gCZaVrCcAJa\\\\xD_[rDoFn@gLtAoH|@\",\"name\":\"3rd Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.402041,37.786261],\"bearing_before\":225.0,\"bearing_after\":313.0,\"instruction\":\"Turn right onto 3rd Street\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":428.7,\"announcement\":\"In a quarter mile, turn left onto Sutter Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, turn left onto Sutter Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":55.3,\"announcement\":\"Turn left onto Sutter Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Sutter Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":448.7,\"primary\":{\"text\":\"Sutter Street\",\"components\":[{\"text\":\"Sutter Street\",\"type\":\"text\",\"abbr\":\"Sutter St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":214.4,\"intersections\":[{\"location\":[-122.402041,37.786261],\"bearings\":[45,135,225,315],\"entry\":[false,false,true,true],\"in\":0,\"out\":3},{\"location\":[-122.403436,37.787676],\"bearings\":[180,345],\"entry\":[false,true],\"in\":0,\"out\":1},{\"location\":[-122.403497,37.787965],\"bearings\":[165,255,345],\"entry\":[false,true,true],\"in\":0,\"out\":2},{\"location\":[-122.403684,37.788901],\"bearings\":[75,165,255,345],\"entry\":[true,false,false,true],\"in\":1,\"out\":3}]},{\"distance\":1301.1,\"duration\":301.2,\"geometry\":\"qgoagA~`}mhFxAnU`D~f@ZnErBt[pChb@bCz^hBtXLxB\\\\fFz@xMnHziANbCB`@Bb@VxDlAhRzDdm@zBv]tAjTbEfo@fCd`@pAbSlIpqAlAdRrIprAfArPxIjtAbAhOlEvq@nClb@\",\"name\":\"Sutter Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.403872,37.789833],\"bearing_before\":350.0,\"bearing_after\":260.0,\"instruction\":\"Turn left onto Sutter Street\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":1281.1,\"announcement\":\"Continue on Sutter Street for 1 mile\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue on Sutter Street for 1 mile\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":302.4,\"announcement\":\"In 1000 feet, turn right onto Larkin Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 1000 feet, turn right onto Larkin Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":64.8,\"announcement\":\"Turn right onto Larkin Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Larkin Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":1301.1,\"primary\":{\"text\":\"Larkin Street\",\"components\":[{\"text\":\"Larkin Street\",\"type\":\"text\",\"abbr\":\"Larkin St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":422.7,\"intersections\":[{\"location\":[-122.403872,37.789833],\"bearings\":[75,165,255,345],\"entry\":[false,false,true,true],\"in\":1,\"out\":2},{\"location\":[-122.405435,37.789635],\"bearings\":[75,165,255,345],\"entry\":[false,true,true,true],\"in\":0,\"out\":2},{\"location\":[-122.40651,37.789496],\"bearings\":[75,255,270],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-122.406982,37.789436],\"bearings\":[75,165,255,345],\"entry\":[false,true,true,false],\"in\":0,\"out\":2},{\"location\":[-122.408616,37.789229],\"bearings\":[75,165,255,345],\"entry\":[false,true,true,true],\"in\":0,\"out\":2},{\"location\":[-122.410267,37.78902],\"bearings\":[75,165,255,345],\"entry\":[false,true,true,false],\"in\":0,\"out\":2},{\"location\":[-122.411912,37.788811],\"bearings\":[75,165,255,345],\"entry\":[false,false,true,true],\"in\":0,\"out\":2},{\"location\":[-122.413555,37.788603],\"bearings\":[75,165,255,345],\"entry\":[false,true,true,false],\"in\":0,\"out\":2},{\"location\":[-122.415199,37.788394],\"bearings\":[75,165,255,345],\"entry\":[false,false,true,true],\"in\":0,\"out\":2},{\"location\":[-122.416847,37.788185],\"bearings\":[75,165,255,345],\"entry\":[false,true,true,false],\"in\":0,\"out\":2}]},{\"distance\":157.5,\"duration\":43.5,\"geometry\":\"oskagAlrynhF}YlDcALc\\\\zDe\\\\|D\",\"name\":\"Larkin Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.418487,37.787976],\"bearing_before\":260.0,\"bearing_after\":350.0,\"instruction\":\"Turn right onto Larkin Street\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":137.5,\"announcement\":\"In 500 feet, turn left onto Frank Norris Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 500 feet, turn left onto Frank Norris Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":54.3,\"announcement\":\"Turn left onto Frank Norris Street, then you will arrive at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Frank Norris Street, then you will arrive at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":157.5,\"primary\":{\"text\":\"Frank Norris Street\",\"components\":[{\"text\":\"Frank Norris Street\",\"type\":\"text\",\"abbr\":\"Frank Norris St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":88.30000000000001,\"intersections\":[{\"location\":[-122.418487,37.787976],\"bearings\":[75,165,255,345],\"entry\":[false,false,true,true],\"in\":0,\"out\":3},{\"location\":[-122.418675,37.788907],\"bearings\":[75,165,255,345],\"entry\":[true,false,false,true],\"in\":1,\"out\":3}]},{\"distance\":58.8,\"duration\":19.2,\"geometry\":\"{jnagAbdznhFfDfh@\",\"name\":\"Frank Norris Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.41877,37.789374],\"bearing_before\":350.0,\"bearing_after\":260.0,\"instruction\":\"Turn left onto Frank Norris Street\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":15.3,\"announcement\":\"You have arrived at your destination, on the left\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYou have arrived at your destination, on the left\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":58.8,\"primary\":{\"text\":\"You will arrive\",\"components\":[{\"text\":\"You will arrive\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":15.3,\"primary\":{\"text\":\"You have arrived\",\"components\":[{\"text\":\"You have arrived\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":19.2,\"intersections\":[{\"location\":[-122.41877,37.789374],\"bearings\":[165,255,345],\"entry\":[false,true,true],\"in\":0,\"out\":1}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"senagAjm{nhF\",\"name\":\"Frank Norris Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.41943,37.78929],\"bearing_before\":261.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination, on the left\",\"type\":\"arrive\",\"modifier\":\"left\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.41943,37.78929],\"bearings\":[81],\"entry\":[true],\"in\":0}]}],\"annotation\":{\"distance\":[43.91662825581704,11.943930596414795,12.720147968330114,34.58112560559672,50.96480933738249,10.349556668565532,11.521647633895794,2.642432939885349,65.69207265970721,13.476454325038304,6.597385456355177,8.739012405049888,11.88211242795126,79.09012265077331,2.5015065323293664,30.82936532272039,98.39828842386073,44.790639941124915,10.693441164520335,11.01261615358321,5.788804795506964,29.922675858136653,33.26909758034983,7.0820541544819315,10.615108370406503,83.67038127825393,56.875124252943365,26.99920350042917,10.975235722547206,11.03736589809851,26.514590403072486,28.469188138310233,55.32738694103918,38.000124789328176,6.18363280790793,4.1374777615485385,4.2439603328353135,3.7216847386479657,6.84150765230015,18.76050555783645,2.453285632444654,2.4721101722571404,3.4658958230385424,10.821479115584705,5.87916513105225,9.9604540665592,7.662847422384856,41.5547475928779,3.8182928761703248,52.36226788538483,50.45344175501242,13.512840023715219,23.88097293019762,17.124574636341766,32.03673260661861,56.97172176771385,9.273097537351157,40.85783149467874,50.321959863407926,45.42531093131366,36.60410752887764,5.418047362235354,10.331833805601269,21.097518237086025,106.65120914643926,5.869165604886359,1.510747584969388,1.5977407698092072,8.282839184318386,27.50496600947513,65.79336436251,43.79269655812305,30.439578896696993,68.7280567535395,47.28335753118788,28.668614505497295,117.59151716170835,27.33160302899699,119.03333443522746,25.109083661088206,121.60342114636497,23.251518618085974,72.28851557063288,50.47875214130949,48.544664511364054,3.8314231434204844,52.485898876378535,52.609613080952975,58.76067751580085],\"congestion\":[\"low\",\"low\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"heavy\",\"heavy\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"heavy\",\"heavy\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"heavy\",\"low\",\"low\",\"low\",\"low\",\"low\"]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":[[-122.3964911,37.7913456],[-122.4193921,37.7891016]],\"alternatives\":true,\"language\":\"en\",\"continue_straight\":false,\"roundabout_exits\":false,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":[\"congestion,distance\"],\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"access_token\":\"pk.eyJ1IjoiY2FmZXNpbGVuY2lvIiwiYSI6ImNqY252N2ZmNTB4dDEyeW8wNTRoOWN0MjcifQ.RlVyJOu8e9R9R2KcaiCyuA\",\"uuid\":\"ck7do38sk01si82otmu389cx2\"},\"voiceLocale\":\"en-US\"}"

    private val locationEngineCallback = MyLocationEngineCallback()
    private lateinit var localLocationEngine: LocationEngine
    private lateinit var mapboxMap: MapboxMap
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_view)

        localLocationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)
        navigationView.onCreate(savedInstanceState)
        navigationView.initialize(this)
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
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        navigationView.onDestroy()
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
                this.mapboxMap = navMapboxMap.retrieveMap()
                //this.mapboxMap.addOnMapLongClickListener(this@NavigationViewActivity)
                navigationView.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it }
                startLocationUpdates()

                val directionsRoute = getDirectionsRoute()
                val optionsBuilder = NavigationViewOptions.builder()
                optionsBuilder.navigationListener(this)
                optionsBuilder.directionsRoute(directionsRoute)
                optionsBuilder.shouldSimulateRoute(true)
                //extractConfiguration(options)
                optionsBuilder.navigationOptions(NavigationOptions.Builder().build())
                navigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

//    override fun onMapLongClick(point: LatLng): Boolean {
//        mapboxMap.locationComponent.lastKnownLocation?.let {
//            origin = Point.fromLngLat(point.longitude, point.latitude)
//        }
//        if (!::destination.isInitialized) {
//            destination = Point.fromLngLat(point.longitude, point.latitude)
//            navigationView.addMarker(destination)
//            //navigationView.showRoutes(origin, destination)
//
//            // next section temporary until I figure out
//            // where the initial route is going to come from that's
//            // needed for NavigationViewOptions
//
////            mapboxNavigation.requestRoutes(
////                    RouteOptions.builder().applyDefaultParams()
////                            .accessToken(Utils.getMapboxAccessToken(applicationContext))
////                            .coordinates(origin, null, destination)
////                            .alternatives(true)
////                            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
////                            .build(),
////                            routesReqCallback
////            )
//
//            // end temporary section
//
//            return true
//        }
//        return false
//    }

//    private val routesReqCallback = object : RoutesRequestCallback {
//        override fun onRoutesReady(routes: List<DirectionsRoute>): List<DirectionsRoute> {
//            Timber.d("route request success %s", routes.toString())
//
////            val optionsBuilder = NavigationViewOptions.builder()
////            optionsBuilder.navigationListener(this@NavigationViewActivity)
////            //extractRoute(this)
////            //extractConfiguration(options)
////            optionsBuilder.directionsRoute(routes[0])
////            optionsBuilder.navigationOptions(NavigationOptions.Builder().build())
////            navigationView.startNavigation(optionsBuilder.build())
////
//            navigationView.drawRoute(routes[0])
//
//            return routes
//        }
//
//        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
//            Timber.e("route request failure %s", throwable.toString())
//        }
//
//        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
//            Timber.d("route request canceled")
//        }
//    }

    private fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(1000L)
            .setFastestInterval(500L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        try {
            localLocationEngine.requestLocationUpdates(
                request,
                locationEngineCallback,
                null
            )
            localLocationEngine.getLastLocation(locationEngineCallback)
        } catch (exception: SecurityException) {
            Timber.e(exception)
        }
    }

    private fun stopLocationUpdates() {
        localLocationEngine.removeLocationUpdates(locationEngineCallback)
    }

    inner class MyLocationEngineCallback : LocationEngineCallback<LocationEngineResult> {

        override fun onSuccess(result: LocationEngineResult?) {
            result?.lastLocation?.let { navigationView.updateNavigationMap(it) }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    override fun onNavigationRunning() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNavigationFinished() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCancelNavigation() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }
}
