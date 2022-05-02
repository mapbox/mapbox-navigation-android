package com.mapbox.androidauto.car.navigation.roadlabel

import android.Manifest
import android.graphics.Color
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.mapbox.androidauto.testing.BitmapTestUtil
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.ui.shield.model.RouteShieldFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@ExperimentalMapboxNavigationAPI
@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class RoadLabelRendererTest {

    @Rule
    @JvmField
    val testName = TestName()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val bitmapTestUtils = BitmapTestUtil(
        "expected_road_label_images",
        "test_road_label_images"
    )

    private val roadLabelBitmapRenderer =
        RoadLabelRenderer(InstrumentationRegistry.getInstrumentation().context.resources)

    @Test
    fun street_with_name() {
        val bitmap = roadLabelBitmapRenderer.render(
            createRoad("Pennsylvania Avenue"),
            emptyList(),
            RoadLabelOptions.Builder()
                .backgroundColor(0x784D4DD3)
                .build()
        )

        bitmapTestUtils.assertBitmapsSimilar(testName, bitmap!!)
    }

    @Test
    fun street_with_numbers() {
        val bitmap = roadLabelBitmapRenderer.render(
            createRoad("11th Street"),
            emptyList(),
            RoadLabelOptions.Builder()
                .backgroundColor(0x784D4DD3)
                .build()
        )

        bitmapTestUtils.assertBitmapsSimilar(testName, bitmap!!)
    }

    @Test
    fun very_long_street_name() {
        val bitmap = roadLabelBitmapRenderer.render(
            createRoad("Taumatawhakatangihangakoauauotamateaturipukakapikimaungahoronukupokaiwhenuakitanatahu"),
            emptyList(),
            RoadLabelOptions.Builder()
                .backgroundColor(0x784D4DD3)
                .build()
        )

        bitmapTestUtils.assertBitmapsSimilar(testName, bitmap!!)
    }

    @Test
    fun blue_label_without_shadow() {
        val bitmap = roadLabelBitmapRenderer.render(
            createRoad("Eu Tong Sen Street"),
            emptyList(),
            RoadLabelOptions.Builder()
                .shadowColor(null)
                .roundedLabelColor(0xFF1A65CA.toInt())
                .textColor(Color.WHITE)
                .build()
        )

        bitmapTestUtils.assertBitmapsSimilar(testName, bitmap!!)
    }

    @Test
    fun street_with_shield() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val byteArray = context.assets.open("shield.svg").use { it.readBytes() }
        val mapboxShield = mockk<MapboxShield>()
        val bitmap = roadLabelBitmapRenderer.render(
            listOf(createComponent("Clarksburg Road"), createComponent("/"), createComponent("121", mapboxShield)),
            listOf(RouteShieldFactory.buildRouteShield("download-url", byteArray, mapboxShield, mockk())),
            RoadLabelOptions.Builder()
                .backgroundColor(0x784D4DD3)
                .build(),
        )

        bitmapTestUtils.assertBitmapsSimilar(testName, bitmap!!)
    }

    private fun createRoad(text: String): List<RoadComponent> {
        return listOf(createComponent(text))
    }

    private fun createComponent(text: String, shield: MapboxShield? = null): RoadComponent {
        return mockk {
            every { this@mockk.text } returns text
            every { this@mockk.shield } returns shield
            every { imageBaseUrl } returns null
        }
    }
}
