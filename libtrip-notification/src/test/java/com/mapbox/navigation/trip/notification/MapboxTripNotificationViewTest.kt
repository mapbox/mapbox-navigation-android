package com.mapbox.navigation.trip.notification

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.text.SpannableString
import android.view.View
import android.widget.RemoteViews
import com.mapbox.navigation.trip.notification.internal.END_NAVIGATION
import com.mapbox.navigation.trip.notification.internal.STOP_SESSION
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MapboxTripNotificationViewTest {

    private lateinit var mockedContext: Context
    private lateinit var collapsedViews: RemoteViews
    private lateinit var expandedViews: RemoteViews

    @Before
    fun setUp() {
        mockedContext = createContext()
        every { mockedContext.applicationContext } returns mockedContext
        mockRemoteViews()
    }

    private fun mockRemoteViews() {
        mockkObject(RemoteViewsProvider)
        collapsedViews = mockk(relaxUnitFun = true)
        expandedViews = mockk(relaxUnitFun = true)
        every {
            RemoteViewsProvider.createRemoteViews(
                any(),
                R.layout.mapbox_notification_navigation_collapsed,
            )
        } returns collapsedViews
        every {
            RemoteViewsProvider.createRemoteViews(
                any(),
                R.layout.mapbox_notification_navigation_expanded,
            )
        } returns expandedViews
    }

    @Test
    fun buildRemoteViews() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
        }

        assertNotNull(view.collapsedView)
        assertNotNull(view.expandedView)
        verify { view.expandedView!!.setOnClickPendingIntent(R.id.endNavigationBtn, pendingIntent) }
    }

    @Test
    fun setVisibility() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.setVisibility(View.VISIBLE)
        }

        verify { view.collapsedView!!.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE) }
        verify { view.expandedView!!.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE) }
    }

    @Test
    fun setEndNavigationButtonText() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.setEndNavigationButtonText(R.string.mapbox_stop_session)
        }

        verify { view.expandedView!!.setTextViewText(R.id.endNavigationBtn, STOP_SESSION) }
    }

    @Test
    fun updateInstructionText() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.updateInstructionText("foobar")
        }

        verify { view.collapsedView!!.setTextViewText(R.id.notificationInstructionText, "foobar") }
        verify { view.expandedView!!.setTextViewText(R.id.notificationInstructionText, "foobar") }
    }

    @Test
    fun updateDistanceText() {
        val pendingIntent = createPendingOpenIntent(mockedContext)
        val text = mockk<SpannableString>()

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.updateDistanceText(text)
        }

        verify {
            view.collapsedView!!.setTextViewText(R.id.notificationDistanceText, text.toString())
        }
        verify {
            view.expandedView!!.setTextViewText(R.id.notificationDistanceText, text.toString())
        }
    }

    @Test
    fun updateArrivalTime() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.updateArrivalTime("foobar")
        }

        verify { view.collapsedView!!.setTextViewText(R.id.notificationArrivalText, "foobar") }
        verify { view.expandedView!!.setTextViewText(R.id.notificationArrivalText, "foobar") }
    }

    @Test
    fun updateImage() {
        val pendingIntent = createPendingOpenIntent(mockedContext)
        val bitmap = mockk<Bitmap>()

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.updateImage(bitmap)
        }

        verify { view.collapsedView!!.setImageViewBitmap(R.id.maneuverImage, bitmap) }
        verify { view.expandedView!!.setImageViewBitmap(R.id.maneuverImage, bitmap) }
    }

    @Test
    fun resetView() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.resetView()
        }

        verify { view.collapsedView!!.setTextViewText(R.id.notificationDistanceText, "") }
        verify { view.collapsedView!!.setTextViewText(R.id.notificationArrivalText, "") }
        verify { view.collapsedView!!.setTextViewText(R.id.notificationInstructionText, "") }
        verify { view.collapsedView!!.setViewVisibility(R.id.etaContent, View.GONE) }
        verify { view.collapsedView!!.setViewVisibility(R.id.freeDriveText, View.GONE) }
        verify {
            view.collapsedView!!.setViewVisibility(R.id.notificationInstructionText, View.GONE)
        }

        verify { view.expandedView!!.setTextViewText(R.id.notificationDistanceText, "") }
        verify { view.expandedView!!.setTextViewText(R.id.notificationArrivalText, "") }
        verify { view.expandedView!!.setTextViewText(R.id.notificationInstructionText, "") }
        verify { view.expandedView!!.setViewVisibility(R.id.etaContent, View.GONE) }
        verify { view.expandedView!!.setViewVisibility(R.id.freeDriveText, View.GONE) }
        verify {
            view.expandedView!!.setViewVisibility(R.id.notificationInstructionText, View.GONE)
        }
    }

    @Test
    fun setFreeDriveMode_true() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.setFreeDriveMode(true)
        }

        verify { view.collapsedView!!.setViewVisibility(R.id.etaContent, View.GONE) }
        verify { view.collapsedView!!.setViewVisibility(R.id.freeDriveText, View.VISIBLE) }
        verify {
            view.collapsedView!!.setViewVisibility(R.id.notificationInstructionText, View.GONE)
        }
        verify {
            view.collapsedView!!.setImageViewResource(
                R.id.maneuverImage,
                R.drawable.mapbox_ic_navigation,
            )
        }
        verify { view.expandedView!!.setViewVisibility(R.id.etaContent, View.GONE) }
        verify { view.expandedView!!.setViewVisibility(R.id.freeDriveText, View.VISIBLE) }
        verify {
            view.expandedView!!.setViewVisibility(R.id.notificationInstructionText, View.GONE)
        }
        verify {
            view.expandedView!!.setImageViewResource(
                R.id.maneuverImage,
                R.drawable.mapbox_ic_navigation,
            )
        }
        verify { view.expandedView!!.setTextViewText(R.id.endNavigationBtn, STOP_SESSION) }
    }

    @Test
    fun setFreeDriveMode_false() {
        val pendingIntent = createPendingOpenIntent(mockedContext)

        val view = MapboxTripNotificationView(mockedContext).also {
            it.buildRemoteViews(pendingIntent)
            it.setFreeDriveMode(false)
        }

        verify { view.collapsedView!!.setViewVisibility(R.id.etaContent, View.VISIBLE) }
        verify { view.collapsedView!!.setViewVisibility(R.id.freeDriveText, View.GONE) }
        verify {
            view.collapsedView!!.setViewVisibility(R.id.notificationInstructionText, View.VISIBLE)
        }
        verify { view.expandedView!!.setViewVisibility(R.id.etaContent, View.VISIBLE) }
        verify { view.expandedView!!.setViewVisibility(R.id.freeDriveText, View.GONE) }
        verify {
            view.expandedView!!.setViewVisibility(R.id.notificationInstructionText, View.VISIBLE)
        }
        verify { view.expandedView!!.setTextViewText(R.id.endNavigationBtn, END_NAVIGATION) }
    }

    private fun createContext(): Context {
        val mockedContext = mockk<Context>()
        val mockedConfiguration = Configuration()
        mockedConfiguration.locale = Locale("en")
        val mockedResources = mockk<Resources>(relaxed = true)
        every { mockedResources.configuration } returns (mockedConfiguration)
        every { mockedContext.resources } returns (mockedResources)
        val mockedPackageManager = mockk<PackageManager>(relaxed = true)
        every { mockedContext.packageManager } returns (mockedPackageManager)
        every { mockedContext.packageName } returns ("com.mapbox.navigation.trip.notification")
        every { mockedContext.getString(R.string.mapbox_stop_session) } returns STOP_SESSION
        every { mockedContext.getString(R.string.mapbox_end_navigation) } returns END_NAVIGATION
        return mockedContext
    }

    private fun createPendingOpenIntent(applicationContext: Context): PendingIntent? {
        val pm = applicationContext.packageManager
        val intent = pm.getLaunchIntentForPackage(applicationContext.packageName) ?: return null
        intent.setPackage(null)
        return PendingIntent.getActivity(applicationContext, 0, intent, 0)
    }
}
