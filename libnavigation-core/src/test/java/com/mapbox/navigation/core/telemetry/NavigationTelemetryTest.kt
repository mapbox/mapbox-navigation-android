package com.mapbox.navigation.core.telemetry

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.telemetry.AndroidAutoEvent
import com.mapbox.navigation.core.internal.telemetry.ExtendedUserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackObserver
import com.mapbox.navigation.core.telemetry.UserFeedback.Companion.mapToNative
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.OuterDeviceAction
import com.mapbox.navigator.Telemetry
import com.mapbox.navigator.UserFeedbackHandle
import com.mapbox.navigator.UserFeedbackMetadata
import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationTelemetryTest {

    private val nativeUserFeedbackCallbackSlot = slot<com.mapbox.navigator.UserFeedbackCallback>()
    private val userFeedbackHandle = mockk<UserFeedbackHandle>(relaxed = true)
    private val feedbackMetadataWrapper = mockk<FeedbackMetadataWrapper>(relaxed = true)

    private val tripSession = mockk<TripSession>(relaxed = true)
    private val nativeNavigator: MapboxNativeNavigator = mockk(relaxed = true)
    private val nativeTelemetry = mockk<Telemetry>(relaxed = true)

    private lateinit var telemetry: NavigationTelemetry

    @Before
    fun setUp() {
        every {
            nativeTelemetry.postUserFeedback(any(), any(), capture(nativeUserFeedbackCallbackSlot))
        } just Runs
        every { nativeTelemetry.startBuildUserFeedbackMetadata() } returns userFeedbackHandle
        every { nativeNavigator.telemetry } returns nativeTelemetry
        telemetry = NavigationTelemetry.create(tripSession, nativeNavigator)

        mockkObject(FeedbackMetadataWrapper)
        every { FeedbackMetadataWrapper.create(any()) } returns feedbackMetadataWrapper
    }

    @After
    fun tearDown() {
        unmockkObject(FeedbackMetadataWrapper)
    }

    @Test
    fun `returns expected metadata wrapper if trip session is started`() {
        every { tripSession.getState() } returns TripSessionState.STARTED

        assertEquals(
            feedbackMetadataWrapper,
            telemetry.provideFeedbackMetadataWrapper(),
        )

        verify(exactly = 1) {
            FeedbackMetadataWrapper.create(eq(userFeedbackHandle))
        }
    }

    @Test
    fun `throws exception when feedback metadata requested if trip session is not started`() {
        every { tripSession.getState() } returns TripSessionState.STOPPED

        val e = assertThrows(IllegalStateException::class.java) {
            telemetry.provideFeedbackMetadataWrapper()
        }

        assertEquals(
            "Feedback metadata can only be provided when the trip session is started",
            e.message,
        )
    }

    @Test
    fun `calls native postTelemetryCustomEvent() when postCustomEvent() called`() {
        telemetry.postCustomEvent("test-type", "test-version", "test-payload")
        verify(exactly = 1) {
            nativeTelemetry.postTelemetryCustomEvent(
                eq("test-type"),
                eq("test-version"),
                eq("test-payload"),
            )
        }
    }

    @Test
    fun `calls postOuterDeviceEvent(CONNECTED) event when postAndroidAutoEvent(CONNECTED) called`() {
        telemetry.postAndroidAutoEvent(AndroidAutoEvent.CONNECTED)
        verify(exactly = 1) {
            nativeTelemetry.postOuterDeviceEvent(
                eq(OuterDeviceAction.CONNECTED),
            )
        }
    }

    @Test
    fun `calls postOuterDeviceEvent(DISCONNECTED) event when postAndroidAutoEvent(DISCONNECTED) called`() {
        telemetry.postAndroidAutoEvent(AndroidAutoEvent.DISCONNECTED)
        verify(exactly = 1) {
            nativeTelemetry.postOuterDeviceEvent(
                eq(OuterDeviceAction.DISCONNECTED),
            )
        }
    }

    @Test
    fun `calls native postUserFeedback() when platform postUserFeedback() called`() {
        telemetry.postUserFeedback(TEST_USER_FEEDBACK, null)

        nativeUserFeedbackCallbackSlot.captured.run(ExpectedFactory.createValue(TEST_LOCATION))

        verify(exactly = 1) {
            nativeTelemetry.postUserFeedback(
                eq(TEST_NATIVE_FEEDBACK_METADATA),
                eq(TEST_USER_FEEDBACK.mapToNative()),
                eq(nativeUserFeedbackCallbackSlot.captured),
            )
        }
    }

    @Test
    fun `calls back when postUserFeedback() succeeded`() {
        val callback = mockk<(ExtendedUserFeedback) -> Unit>(relaxed = true)
        val registeredCallback = mockk<UserFeedbackObserver>(relaxed = true)

        telemetry.registerUserFeedbackObserver(registeredCallback)

        telemetry.postUserFeedback(TEST_USER_FEEDBACK, callback)
        nativeUserFeedbackCallbackSlot.captured.run(ExpectedFactory.createValue(TEST_LOCATION))

        verify(exactly = 1) {
            callback.invoke(eq(TEST_EXTENDED_USER_FEEDBACK))
            registeredCallback.onNewUserFeedback(eq(TEST_EXTENDED_USER_FEEDBACK))
        }
    }

    @Test
    fun `doesn't call back when postUserFeedback() failed`() {
        val callback = mockk<(ExtendedUserFeedback) -> Unit>(relaxed = true)
        val registeredCallback = mockk<UserFeedbackObserver>(relaxed = true)

        telemetry.registerUserFeedbackObserver(registeredCallback)

        telemetry.postUserFeedback(TEST_USER_FEEDBACK, callback)
        nativeUserFeedbackCallbackSlot.captured.run(ExpectedFactory.createError("Error"))

        verify {
            callback wasNot called
            registeredCallback wasNot called
        }
    }

    @Test
    fun `doesn't call unregistered callbacks when postUserFeedback() called`() {
        val registeredCallback = mockk<UserFeedbackObserver>(relaxed = true)

        telemetry.registerUserFeedbackObserver(registeredCallback)
        telemetry.unregisterUserFeedbackObserver(registeredCallback)

        telemetry.postUserFeedback(TEST_USER_FEEDBACK, null)
        nativeUserFeedbackCallbackSlot.captured.run(ExpectedFactory.createValue(TEST_LOCATION))

        verify(exactly = 0) {
            registeredCallback.onNewUserFeedback(any())
        }
    }

    @Test
    fun `clears all callbacks when clearObservers() called`() {
        val callback1 = mockk<UserFeedbackObserver>(relaxed = true)
        val callback2 = mockk<UserFeedbackObserver>(relaxed = true)

        telemetry.registerUserFeedbackObserver(callback1)
        telemetry.registerUserFeedbackObserver(callback2)
        telemetry.clearObservers()

        telemetry.postUserFeedback(TEST_USER_FEEDBACK, null)
        nativeUserFeedbackCallbackSlot.captured.run(ExpectedFactory.createValue(TEST_LOCATION))

        verify(exactly = 0) {
            callback1.onNewUserFeedback(any())
            callback2.onNewUserFeedback(any())
        }
    }

    @Test
    fun `uses actual native telemetry after navigator recreation`() {
        val newNativeTelemetry = mockk<Telemetry>(relaxed = true)
        every { nativeNavigator.telemetry } returns newNativeTelemetry

        telemetry.postAndroidAutoEvent(AndroidAutoEvent.CONNECTED)

        verify(exactly = 0) { nativeTelemetry.postOuterDeviceEvent(any()) }
        verify(exactly = 1) { newNativeTelemetry.postOuterDeviceEvent(OuterDeviceAction.CONNECTED) }
    }

    private companion object {

        val TEST_LOCATION: Point = Point.fromLngLat(1.0, 2.0)
        val TEST_NATIVE_FEEDBACK_METADATA = mockk<UserFeedbackMetadata>()
        val TEST_FEEDBACK_METADATA = FeedbackMetadata.create(TEST_NATIVE_FEEDBACK_METADATA)
        val TEST_USER_FEEDBACK = UserFeedback.Builder(
            FeedbackEvent.INCORRECT_VISUAL,
            "test-description",
        )
            .feedbackSubTypes(listOf(FeedbackEvent.TURN_ICON_INCORRECT))
            .feedbackMetadata(TEST_FEEDBACK_METADATA)
            .build()

        val TEST_EXTENDED_USER_FEEDBACK = ExtendedUserFeedback(
            feedback = TEST_USER_FEEDBACK,
            feedbackId = "-1",
            location = TEST_LOCATION,
        )
    }
}
