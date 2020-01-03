package com.mapbox.navigation

import android.app.NotificationManager
import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.trip.notification.NavigationNotificationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class NavigationControllerTest {

    private lateinit var navigationController: NavigationController
    private val context: Context = mockk(relaxed = true)
    private val accessToken = "pk.XXX"
    private val navigator: MapboxNativeNavigator = mockk()
    private val locationEngine: LocationEngine = mockk()
    private val locationEngineRequest: LocationEngineRequest = mockk()
    private val navigationNotificationProvider: NavigationNotificationProvider = mockk()
    private val tripServiceLambda: () -> Unit = mockk()

    @Before
    fun setUp() {
        val notificationManager = mockk<NotificationManager>()
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

        navigationController =
            NavigationController(
                context,
                accessToken,
                navigator,
                locationEngine,
                locationEngineRequest,
                tripServiceLambda,
                navigationNotificationProvider
            )
    }

    @Test
    fun sanity() {
        Assert.assertNotNull(navigationController)
    }
}
