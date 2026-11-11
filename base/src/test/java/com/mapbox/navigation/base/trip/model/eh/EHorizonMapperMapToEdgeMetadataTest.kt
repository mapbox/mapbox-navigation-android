package com.mapbox.navigation.base.trip.model.eh

import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigator.EdgeMetadata
import com.mapbox.navigator.FunctionalRoadClass
import com.mapbox.navigator.RoadName
import com.mapbox.navigator.RoadSurface
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EHorizonMapperMapToEdgeMetadataTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `names keep their text and language`() {
        val metadata = edgeMetadata(RoadName("Main Street", "en", null, null))

        val names = metadata.mapToEHorizonEdgeMetadata().names

        assertEquals(listOf("Main Street"), names.map { it.text })
        assertEquals(listOf("en"), names.map { it.language })
    }

    @Test
    fun `name without text is skipped and the other names are kept`() {
        val metadata = edgeMetadata(
            RoadName("Main Street", "en", null, null),
            RoadName("", "de", null, null).withNull("text"),
        )

        val names = metadata.mapToEHorizonEdgeMetadata().names

        assertEquals(listOf("Main Street"), names.map { it.text })
    }

    @Test
    fun `name without language is skipped`() {
        val metadata = edgeMetadata(RoadName("Main Street", "", null, null).withNull("language"))

        val names = metadata.mapToEHorizonEdgeMetadata().names

        assertEquals(emptyList<String>(), names.map { it.text })
    }

    private fun edgeMetadata(vararg names: RoadName): EdgeMetadata =
        mockk(relaxed = true) {
            every { frc } returns FunctionalRoadClass.SECONDARY
            every { surface } returns RoadSurface.PAVED
            every { this@mockk.names } returns names.toList()
        }

    // the native model declares these fields non-null, so a null one can only be set reflectively
    private fun RoadName.withNull(field: String): RoadName = apply {
        RoadName::class.java.getDeclaredField(field).apply { isAccessible = true }.set(this, null)
    }
}
