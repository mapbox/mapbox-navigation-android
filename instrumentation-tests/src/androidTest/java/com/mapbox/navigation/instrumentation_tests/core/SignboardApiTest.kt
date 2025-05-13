package com.mapbox.navigation.instrumentation_tests.core

import android.os.Looper
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.ui.maps.guidance.signboard.api.MapboxExternalFileResolver
import com.mapbox.navigation.ui.maps.guidance.signboard.api.MapboxSignboardApi
import com.mapbox.navigation.ui.maps.guidance.signboard.api.MapboxSvgToBitmapParser
import com.mapbox.navigation.ui.maps.guidance.signboard.api.SvgToBitmapParser
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import com.mapbox.navigation.ui.maps.guidance.signboard.view.MapboxSignboardView
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import okio.source
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class SignboardApiTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation() = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @Test
    fun generate_signboard_from_banner_instructions_and_show() {
        sdkTest {
            mockWebServerRule.requestHandlers.add {
                val file =
                    InstrumentationRegistry.getInstrumentation().context.assets.open("shield.svg")
                        .source()
                val body = Buffer().apply {
                    writeAll(file)
                }
                MockResponse().setBody(body)
            }

            val defaultParser = MapboxSvgToBitmapParser(MapboxExternalFileResolver(context.assets))
            val parserWrapper =
                SvgToBitmapParser { svg: ByteBuffer, options: MapboxSignboardOptions ->
                    // Make sure parsing is not done on the main thread
                    Assert.assertNotEquals(
                        "Parsing must be done on a worker thread",
                        Thread.currentThread(),
                        Looper.getMainLooper().thread,
                    )
                    defaultParser.parse(svg, options)
                }

            val bannerInstructionsWithSignboard = BannerInstructions.builder().apply {
                distanceAlongGeometry(1.0)
                primary(BannerText.builder().text("Pennsylvania Avenue Northwest").build())
                val bannerComponents = listOf(
                    BannerComponents.builder()
                        .type(BannerComponents.GUIDANCE_VIEW)
                        .subType(BannerComponents.SIGNBOARD)
                        .imageUrl(
                            mockWebServerRule.webServer.url("/shield.svg").toString(),
                        )
                        .text("Shield 121")
                        .build(),
                )
                view(
                    BannerView.builder()
                        .components(bannerComponents)
                        .text("Banner View text")
                        .build(),
                )
            }.build()

            val result: Expected<SignboardError, SignboardValue> =
                suspendCancellableCoroutine { continuation ->
                    val api = MapboxSignboardApi(parserWrapper)
                    api.generateSignboard(bannerInstructionsWithSignboard) {
                            result: Expected<SignboardError, SignboardValue> ->
                        // Make sure response is communicated on the main thread
                        Assert.assertEquals(
                            "Result must be called on main thread",
                            Thread.currentThread(),
                            Looper.getMainLooper().thread,
                        )
                        continuation.resume(result)
                    }
                    continuation.invokeOnCancellation { api.cancelAll() }
                }
            Assert.assertTrue("Error: ${result.error?.errorMessage}", result.isValue)
            val mapboxSignboardView = MapboxSignboardView(activity)
            mapboxSignboardView.render(result)
            mapboxSignboardView.id = 123
            activity.binding.root.addView(
                mapboxSignboardView,
                100,
                100,
            )
        }
        // Use Espresso to check the mapboxSignboardView is fully displayed
        Espresso.onView(ViewMatchers.withId(123))
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
            .check { view, _ ->
                Assert.assertEquals(100, view.width)
                Assert.assertEquals(100, view.height)
            }
    }
}
