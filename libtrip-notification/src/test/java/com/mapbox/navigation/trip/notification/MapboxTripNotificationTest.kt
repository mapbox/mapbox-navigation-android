package com.mapbox.navigation.trip.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.format.DateFormat
import com.google.gson.Gson
import com.mapbox.navigation.base.model.route.Route
import com.mapbox.navigation.base.model.route.RouteResponse
import com.mapbox.navigation.base.options.TripNavigationOptions
import com.mapbox.navigation.utils.extensions.ifNonNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapboxTripNotificationTest : BaseTest() {

    private val DIRECTIONS_ROUTE_FIXTURE = "directions_v5_precision_6.json"
    private var route: Route? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        mockkStatic(DateFormat::class)
        mockkStatic(PendingIntent::class)
        val json = loadJsonFixture(DIRECTIONS_ROUTE_FIXTURE)
        val gson = Gson()
        val response = gson.fromJson<RouteResponse>(json, RouteResponse::class.java)
        route = response.routes()[0]
    }

    @Test
    @Throws(Exception::class)
    fun checksArrivalTime() {
        val mockedContext = createContext()
        val navigationOptions = mockk<TripNavigationOptions>(relaxed = true)
        ifNonNull(route) { route ->
            val mapboxNavigationNotification = MapboxTripNotification(mockedContext,
                    navigationOptions, route)
            val routeProgress = buildDefaultTestRouteProgress()
            val mockedTime = Calendar.getInstance()
            mockedTime.timeZone = TimeZone.getTimeZone("UTC")
            val aprilFifteenThreeFourtyFourFiftyThreePmTwoThousandNineteen = 1555357493308L
            mockedTime.timeInMillis = aprilFifteenThreeFourtyFourFiftyThreePmTwoThousandNineteen

            val formattedArrivalTime = mapboxNavigationNotification.generateArrivalTime(routeProgress, mockedTime)
            assertEquals("8:46 pm ETA", formattedArrivalTime)
        }
    }

    private fun createContext(): Context {
        val mockedContext = mockk<Context>()
        val mockedConfiguration = Configuration()
        mockedConfiguration.locale = Locale("en")
        val mockedResources = mockk<Resources>(relaxed = true)
        every { mockedResources.configuration } returns(mockedConfiguration)
        every { mockedContext.resources } returns(mockedResources)
        val mockedPackageManager = mockk<PackageManager>(relaxed = true)
        every { mockedContext.packageManager } returns (mockedPackageManager)
        every { mockedContext.packageName } returns ("com.mapbox.navigation.trip.notification")
        every { mockedContext.getString(any()) } returns ("%s 454545 ETA")
        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every { mockedContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns (notificationManager)
        every { DateFormat.is24HourFormat(mockedContext) } returns(false)
        /*val mockIntent = mockk<Intent>(relaxed = true)
        every {
            PendingIntent.getActivity(any(), any(), any(), any())
        } returns (Uri("http", "", ""))*/
        return mockedContext
    }
}
