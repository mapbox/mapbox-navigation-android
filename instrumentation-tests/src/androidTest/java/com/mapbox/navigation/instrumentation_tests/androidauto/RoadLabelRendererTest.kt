package com.mapbox.navigation.instrumentation_tests.androidauto

import android.Manifest
import android.graphics.Color
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.mapbox.androidauto.car.navigation.roadlabel.RoadLabelOptions
import com.mapbox.androidauto.car.navigation.roadlabel.RoadLabelRenderer
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSpriteAttribute
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadFactory
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.instrumentation_tests.utils.BitmapTestUtil
import com.mapbox.navigation.ui.shield.model.RouteShieldFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

@ExperimentalMapboxNavigationAPI
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
        "androidauto/expected_road_label_images",
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
            createRoad(
                "Taumatawhakatangihangakoauauotamateaturipukakapikimaungahoronukupokaiw" +
                    "henuakitanatahu"
            ),
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
        val byteArray = context.assets.open("androidauto/shield.svg").use { it.readBytes() }
        val mapboxShield = createMapboxShield()
        val bitmap = roadLabelBitmapRenderer.render(
            listOf(
                RoadFactory.buildRoadComponent(text = "Clarksburg Road"),
                RoadFactory.buildRoadComponent(text = "/"),
                RoadFactory.buildRoadComponent(text = "121", shield = mapboxShield)
            ),
            listOf(
                RouteShieldFactory.buildRouteShield(
                    "download-url",
                    byteArray,
                    mapboxShield,
                    createShieldSprite()
                )
            ),
            RoadLabelOptions.Builder()
                .backgroundColor(0x784D4DD3)
                .build(),
        )

        bitmapTestUtils.assertBitmapsSimilar(testName, bitmap!!)
    }

    private fun createRoad(text: String): List<RoadComponent> {
        return listOf(RoadFactory.buildRoadComponent(text = text))
    }

    private fun createMapboxShield(): MapboxShield {
        return MapboxShield.fromJson(
            """{"base_url":"","name":"","text_color":"","display_ref":""}""".trimIndent()
        )
    }

    private fun createShieldSprite(): ShieldSprite {
        return ShieldSprite.builder()
            .spriteAttributes(
                ShieldSpriteAttribute.fromJson(
                    """{"width":0,"height":0,"x":0,"y":0,"pixelRatio":0,"visible":false}"""
                )
            )
            .spriteName("")
            .build()
    }
}
