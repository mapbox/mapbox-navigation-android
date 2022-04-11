package com.mapbox.navigation.trip.notification.internal

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.SpannableString
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.internal.time.TimeFormatter
import com.mapbox.navigation.base.internal.trip.notification.TripNotificationInterceptorOwner
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.trip.notification.R
import com.mapbox.navigation.trip.notification.RemoteViewsProvider
import com.mapbox.navigation.utils.internal.NOTIFICATION_ID
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

internal const val STOP_SESSION = "Stop session"
internal const val END_NAVIGATION = "End Navigation"
internal const val FORMAT_STRING = "%s 454545 ETA"
internal const val MANEUVER_TYPE = "MANEUVER TYPE"
internal const val MANEUVER_MODIFIER = "MANEUVER MODIFIER"
internal const val NAVIGATION_IS_STARTING = "Navigation is starting…"

class MapboxTripNotificationTest {

    private lateinit var notification: MapboxTripNotification
    private lateinit var mockedContext: Context
    private lateinit var collapsedViews: RemoteViews
    private lateinit var expandedViews: RemoteViews
    private val navigationOptions: NavigationOptions = mockk(relaxed = true)
    private val interceptorOwner: TripNotificationInterceptorOwner = mockk(relaxed = true)
    private val distanceSpannable: SpannableString = mockk()
    private val distanceFormatter: DistanceFormatter

    init {
        val distanceSlot = slot<Double>()
        distanceFormatter = mockk()
        every { distanceFormatter.formatDistance(capture(distanceSlot)) } returns distanceSpannable
    }

    @Before
    fun setUp() {
        mockkStatic(DateFormat::class)
        mockkStatic(PendingIntent::class)
        mockkStatic(TimeFormatter::class)
        mockedContext = createContext()
        every { mockedContext.applicationContext } returns mockedContext
        every { navigationOptions.applicationContext } returns mockedContext
        mockRemoteViews()
        notification = MapboxTripNotification(
            navigationOptions,
            interceptorOwner,
            distanceFormatter
        )
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun mockRemoteViews() {
        mockkObject(RemoteViewsProvider)
        collapsedViews = mockk(relaxUnitFun = true)
        expandedViews = mockk(relaxUnitFun = true)
        every {
            RemoteViewsProvider.createRemoteViews(
                any(),
                R.layout.mapbox_notification_navigation_collapsed
            )
        } returns collapsedViews
        every {
            RemoteViewsProvider.createRemoteViews(
                any(),
                R.layout.mapbox_notification_navigation_expanded
            )
        } returns expandedViews
    }

    @Test
    fun generateSanityTest() {
        assertNotNull(notification)
    }

    private fun createContext(): Context {
        val mockedContext = mockk<Context>()
        val mockedBroadcastReceiverIntent = mockk<Intent>()
        val mockPendingIntentForActivity = mockk<PendingIntent>(relaxed = true)
        val mockPendingIntentForBroadcast = mockk<PendingIntent>(relaxed = true)
        val mockedConfiguration = Configuration()
        mockedConfiguration.locale = Locale("en")
        val mockedResources = mockk<Resources>(relaxed = true)
        every { mockedResources.configuration } returns (mockedConfiguration)
        every { mockedContext.resources } returns (mockedResources)
        val mockedPackageManager = mockk<PackageManager>(relaxed = true)
        every { mockedContext.packageManager } returns (mockedPackageManager)
        every { mockedContext.packageName } returns ("com.mapbox.navigation.trip.notification")
        every { mockedContext.getString(any()) } returns FORMAT_STRING
        every { mockedContext.getString(R.string.mapbox_stop_session) } returns STOP_SESSION
        every { mockedContext.getString(R.string.mapbox_end_navigation) } returns END_NAVIGATION
        every {
            mockedContext.getString(R.string.mapbox_navigation_is_starting)
        } returns NAVIGATION_IS_STARTING
        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every {
            mockedContext.getSystemService(Context.NOTIFICATION_SERVICE)
        } returns (notificationManager)
        every { DateFormat.is24HourFormat(mockedContext) } returns (false)
        every {
            PendingIntent.getActivity(
                mockedContext,
                any(),
                any(),
                any()
            )
        } returns (mockPendingIntentForActivity)
        every {
            PendingIntent.getBroadcast(
                mockedContext,
                any(),
                any(),
                any()
            )
        } returns (mockPendingIntentForBroadcast)
        every {
            mockedContext.registerReceiver(
                any(),
                any()
            )
        } returns (mockedBroadcastReceiverIntent)
        every { mockedContext.unregisterReceiver(any()) } just Runs
        return mockedContext
    }

    @Test
    fun whenTripStartedThenRegisterReceiverCalledOnce() {
        notification.onTripSessionStarted()
        verify(exactly = 1) { mockedContext.registerReceiver(any(), any()) }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun whenTripStoppedThenCleanupIsDone() {
        val notificationManager =
            mockedContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notification.onTripSessionStopped()

        verify(exactly = 1) { mockedContext.unregisterReceiver(any()) }
        verify(exactly = 1) { notificationManager.cancel(NOTIFICATION_ID) }
        assertEquals(
            true,
            MapboxTripNotification.notificationActionButtonChannel.isClosedForReceive
        )
        assertEquals(true, MapboxTripNotification.notificationActionButtonChannel.isClosedForSend)
    }

    @Test
    fun getNotificationCreatesBuilderWithDefaults() {
        mockkObject(NotificationBuilderProvider)
        val builder = mockNotificationBuilder()

        notification.getNotification()

        verify {
            builder.setCategory(NotificationCompat.CATEGORY_SERVICE)
            builder.priority = NotificationCompat.PRIORITY_MAX
            builder.setSmallIcon(R.drawable.mapbox_ic_navigation)
            builder.setCustomContentView(any())
            builder.setCustomBigContentView(any())
            builder.setOngoing(true)
            builder.setContentIntent(any())
        }
    }

    @Test
    fun whenUpdateNotificationCalledThenPrimaryTextIsSetToRemoteViews() {
        val state = mockk<TripNotificationState.TripNotificationData>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(state, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.updateNotification(state)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 1) {
            collapsedViews.setTextViewText(R.id.notificationInstructionText, primaryText())
        }
        verify(exactly = 1) {
            expandedViews.setTextViewText(R.id.notificationInstructionText, primaryText())
        }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), END_NAVIGATION) }
        verify(exactly = 0) { expandedViews.setTextViewText(any(), STOP_SESSION) }
        assertEquals(notification.currentManeuverType, MANEUVER_TYPE)
        assertEquals(notification.currentManeuverModifier, MANEUVER_MODIFIER)
    }

    @Test
    fun whenUpdateNotificationCalledThenDistanceTextIsSetToRemoteViews() {
        val distance = 30.0
        val duration = 112.4
        val distanceText = distanceSpannable.toString()
        val state = mockk<TripNotificationState.TripNotificationData>(relaxed = true) {
            every { distanceRemaining } returns distance
            every { durationRemaining } returns duration
        }
        mockUpdateNotificationAndroidInteractions()

        notification.updateNotification(state)

        verify(exactly = 1) {
            collapsedViews.setTextViewText(
                R.id.notificationDistanceText,
                distanceText
            )
        }
        verify(exactly = 1) {
            expandedViews.setTextViewText(
                R.id.notificationDistanceText,
                distanceText
            )
        }
    }

    @Test
    fun whenUpdateNotificationCalledThenArrivalTimeIsSetToRemoteViews() {
        val distance = 30.0
        val duration = 112.4
        mockUpdateNotificationAndroidInteractions()
        val suffix = "this is nice formatting"
        mockTimeFormatter(suffix)
        val result = String.format(FORMAT_STRING, suffix + duration.toString())
        val state = buildTripNotificationState(
            null,
            distance,
            duration,
            null
        )

        notification.updateNotification(state)

        verify(exactly = 1) { collapsedViews.setTextViewText(any(), result) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), result) }
    }

    @Test
    fun whenUpdateNotificationCalledTwiceWithSameDataThenRemoteViewUpdatedTwice() {
        val state = mockk<TripNotificationState.TripNotificationData>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(state, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.updateNotification(state)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 1) { collapsedViews.setTextViewText(any(), primaryText()) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), primaryText()) }

        notification.updateNotification(state)

        verify(exactly = 2) { bannerText.text() }
        verify(exactly = 2) { collapsedViews.setTextViewText(any(), primaryText()) }
        verify(exactly = 2) { expandedViews.setTextViewText(any(), primaryText()) }
        assertEquals(notification.currentManeuverType, MANEUVER_TYPE)
        assertEquals(notification.currentManeuverModifier, MANEUVER_MODIFIER)
    }

    @Test
    fun whenUpdateNotificationCalledTwiceWithDifferentDataThenRemoteViewUpdatedTwice() {
        val state = mockk<TripNotificationState.TripNotificationData>(relaxed = true)
        val initialPrimaryText = "Primary Text"
        val changedPrimaryText = "Changed Primary Text"
        var primaryText = initialPrimaryText
        val primaryTextLambda = { primaryText }
        val bannerText = mockBannerText(state, primaryTextLambda)
        mockUpdateNotificationAndroidInteractions()

        notification.updateNotification(state)
        primaryText = changedPrimaryText
        notification.updateNotification(state)

        verify(exactly = 2) { bannerText.text() }
        verify(exactly = 1) { collapsedViews.setTextViewText(any(), initialPrimaryText) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), initialPrimaryText) }
        verify(exactly = 1) { collapsedViews.setTextViewText(any(), changedPrimaryText) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), changedPrimaryText) }
        assertEquals(notification.currentManeuverType, MANEUVER_TYPE)
        assertEquals(notification.currentManeuverModifier, MANEUVER_MODIFIER)
    }

    @Test
    fun whenGoThroughStartUpdateStopCycleThenNotificationCacheDropped() {
        val state = mockk<TripNotificationState.TripNotificationData>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(state, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.onTripSessionStarted()
        notification.updateNotification(state)
        notification.onTripSessionStopped()
        notification.onTripSessionStarted()

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 1) { collapsedViews.setTextViewText(any(), primaryText()) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), primaryText()) }
        assertNull(notification.currentManeuverType)
        assertNull(notification.currentManeuverModifier)

        notification.updateNotification(state)

        verify(exactly = 2) { bannerText.text() }
        verify(exactly = 2) { collapsedViews.setTextViewText(any(), primaryText()) }
        verify(exactly = 2) { expandedViews.setTextViewText(any(), primaryText()) }
        assertEquals(notification.currentManeuverType, MANEUVER_TYPE)
        assertEquals(notification.currentManeuverModifier, MANEUVER_MODIFIER)
    }

    @Test
    fun whenGoThroughStartUpdateStopCycleThenStartStopSessionDontAffectRemoteViews() {
        val state = mockk<TripNotificationState.TripNotificationData>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(state, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.onTripSessionStarted()

        notification.updateNotification(state)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 1) { collapsedViews.setTextViewText(any(), primaryText()) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), primaryText()) }
        assertEquals(notification.currentManeuverType, MANEUVER_TYPE)
        assertEquals(notification.currentManeuverModifier, MANEUVER_MODIFIER)

        notification.onTripSessionStopped()
        notification.onTripSessionStarted()

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 1) { collapsedViews.setTextViewText(any(), primaryText()) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), primaryText()) }

        notification.onTripSessionStopped()

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 1) { collapsedViews.setTextViewText(any(), primaryText()) }
        verify(exactly = 1) { expandedViews.setTextViewText(any(), primaryText()) }
        assertNull(notification.currentManeuverType)
        assertNull(notification.currentManeuverModifier)

        // navigationIsStarting
        verify(ordering = Ordering.ORDERED) {
            collapsedViews.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE)
            collapsedViews.setViewVisibility(R.id.navigationIsStarting, View.GONE)
            collapsedViews.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE)
        }
        verify(exactly = 3) {
            collapsedViews.setViewVisibility(R.id.navigationIsStarting, any())
        }
        verify(ordering = Ordering.ORDERED) {
            expandedViews.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE)
            expandedViews.setViewVisibility(R.id.navigationIsStarting, View.GONE)
            expandedViews.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE)
        }
        verify(exactly = 3) {
            expandedViews.setViewVisibility(R.id.navigationIsStarting, any())
        }
        // etaContent
        verify(ordering = Ordering.ORDERED) {
            collapsedViews.setViewVisibility(R.id.etaContent, View.VISIBLE)
            collapsedViews.setViewVisibility(R.id.etaContent, View.GONE)
            collapsedViews.setViewVisibility(R.id.etaContent, View.GONE)
        }
        verify(exactly = 3) {
            collapsedViews.setViewVisibility(R.id.etaContent, any())
        }
        verify(ordering = Ordering.ORDERED) {
            expandedViews.setViewVisibility(R.id.etaContent, View.VISIBLE)
            expandedViews.setViewVisibility(R.id.etaContent, View.GONE)
            expandedViews.setViewVisibility(R.id.etaContent, View.GONE)
        }
        verify(exactly = 3) {
            expandedViews.setViewVisibility(R.id.etaContent, any())
        }
        // freeDriveText
        verify(ordering = Ordering.ORDERED) {
            collapsedViews.setViewVisibility(R.id.freeDriveText, View.GONE)
            collapsedViews.setViewVisibility(R.id.freeDriveText, View.GONE)
            collapsedViews.setViewVisibility(R.id.freeDriveText, View.GONE)
        }
        verify(exactly = 3) {
            collapsedViews.setViewVisibility(R.id.freeDriveText, any())
        }
        verify(ordering = Ordering.ORDERED) {
            expandedViews.setViewVisibility(R.id.freeDriveText, View.GONE)
            expandedViews.setViewVisibility(R.id.freeDriveText, View.GONE)
            expandedViews.setViewVisibility(R.id.freeDriveText, View.GONE)
        }
        verify(exactly = 3) {
            expandedViews.setViewVisibility(R.id.freeDriveText, any())
        }
        verify(exactly = 2) {
            expandedViews.setTextViewText(R.id.endNavigationBtn, STOP_SESSION)
        }
    }

    @Test
    fun whenFreeDrive() {
        val nullRouteProgress = null
        mockUpdateNotificationAndroidInteractions()

        notification.onTripSessionStarted()
        notification.updateNotification(buildTripNotificationState(nullRouteProgress))

        verify(ordering = Ordering.ORDERED) {
            collapsedViews.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE)
            collapsedViews.setViewVisibility(R.id.navigationIsStarting, View.GONE)
        }
        verify(exactly = 2) {
            collapsedViews.setViewVisibility(R.id.navigationIsStarting, any())
        }
        verify(ordering = Ordering.ORDERED) {
            expandedViews.setViewVisibility(R.id.navigationIsStarting, View.VISIBLE)
            expandedViews.setViewVisibility(R.id.navigationIsStarting, View.GONE)
        }
        verify(exactly = 2) {
            expandedViews.setViewVisibility(R.id.navigationIsStarting, any())
        }
        verify(exactly = 0) { expandedViews.setTextViewText(any(), END_NAVIGATION) }
        verify(exactly = 2) { expandedViews.setTextViewText(any(), STOP_SESSION) }
    }

    @Test
    fun useInterceptorOwnerInterceptorToBuildNotification() {
        mockkObject(NotificationBuilderProvider)
        every { NotificationBuilderProvider.create(any(), any()) } returns mockk(relaxed = true)
        val notificationBuilderSlot = slot<NotificationCompat.Builder>()
        every { interceptorOwner.interceptor } returns mockk(relaxed = true) {
            every { intercept(capture(notificationBuilderSlot)) } answers { firstArg() }
        }

        val state = buildTripNotificationState(
            null, 10.0, 10.0, null
        )
        notification.updateNotification(state)

        assertTrue(notificationBuilderSlot.isCaptured)
    }

    @Test
    fun theInterceptorCanModifyTheExtender() {
        mockkObject(NotificationBuilderProvider)
        every { NotificationBuilderProvider.create(any(), any()) } returns mockk(relaxed = true)
        val notificationBuilderSlot = slot<NotificationCompat.Builder>()
        every { interceptorOwner.interceptor } returns mockk(relaxed = true) {
            every { intercept(capture(notificationBuilderSlot)) } answers {
                firstArg<NotificationCompat.Builder>().extend(mockk())
            }
        }

        val state = buildTripNotificationState(
            null, 10.0, 10.0, null
        )
        notification.updateNotification(state)

        verify { notificationBuilderSlot.captured.extend(any()) }
    }

    private fun mockUpdateNotificationAndroidInteractions() {
        mockkStatic(TextUtils::class)
        val slot = slot<CharSequence>()
        every { TextUtils.isEmpty(capture(slot)) } answers { slot.captured.isEmpty() }
    }

    private fun mockBannerText(
        state: TripNotificationState.TripNotificationData,
        primaryText: () -> String,
        primaryType: () -> String = { MANEUVER_TYPE },
        primaryModifier: () -> String = { MANEUVER_MODIFIER }
    ): BannerText {
        val bannerText = mockk<BannerText>()
        val bannerInstructions = mockk<BannerInstructions>()
        every { state.bannerInstructions } returns bannerInstructions
        every { bannerInstructions.primary() } returns bannerText
        every { bannerText.degrees() } answers { null }
        every { bannerText.text() } answers { primaryText() }
        every { bannerText.type() } answers { primaryType() }
        every { bannerText.modifier() } answers { primaryModifier() }
        return bannerText
    }

    private fun mockTimeFormatter(@Suppress("SameParameterValue") suffix: String) {
        mockkStatic(TimeFormatter::class)
        val durationSlot = slot<Double>()
        every {
            TimeFormatter.formatTime(any(), capture(durationSlot), any(), any())
        } answers { "$suffix${durationSlot.captured}" }
    }

    private fun mockNotificationBuilder(): NotificationCompat.Builder {
        val builder = mockk<NotificationCompat.Builder>(relaxed = true)
        every { builder.setCategory(any()) } returns builder
        every { builder.setPriority(any()) } returns builder
        every { builder.setSmallIcon(any<Int>()) } returns builder
        every { builder.setCustomContentView(any()) } returns builder
        every { builder.setCustomBigContentView(any()) } returns builder
        every { builder.setOngoing(any()) } returns builder
        every { builder.setColor(any()) } returns builder
        every { builder.setContentIntent(any()) } returns builder
        every { NotificationBuilderProvider.create(any(), any()) } returns builder
        return builder
    }
}
