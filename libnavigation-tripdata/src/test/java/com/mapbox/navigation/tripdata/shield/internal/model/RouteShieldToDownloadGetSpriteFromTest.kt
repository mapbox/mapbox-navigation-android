package com.mapbox.navigation.tripdata.shield.internal.model

import com.mapbox.api.directions.v5.models.ShieldSprite
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class RouteShieldToDownloadGetSpriteFromTest(
    private val shieldToDownloadName: String?,
    private val shieldToDownloadDisplayRef: String?,
    private val input: List<SizeSpecificSpriteInfo>,
    private val expected: ShieldSprite?,
) {

    companion object {

        @Parameterized.Parameters
        @JvmStatic
        fun data(): List<Array<Any?>> {
            val expectedSprite = mockk<ShieldSprite>()
            return listOf(
                // #0
                arrayOf("some name", "123", emptyList<SizeSpecificSpriteInfo>(), null),
                arrayOf(null, "123", emptyList<SizeSpecificSpriteInfo>(), null),
                arrayOf("some name", null, emptyList<SizeSpecificSpriteInfo>(), null),
                arrayOf(null, null, emptyList<SizeSpecificSpriteInfo>(), null),
                arrayOf(
                    null,
                    null,
                    listOf(
                        SizeSpecificSpriteInfo("name1", 3, mockk()),
                        SizeSpecificSpriteInfo("name2", 2, mockk()),
                    ),
                    null,
                ),
                // #5
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name1", 3, mockk()),
                        SizeSpecificSpriteInfo("name2", 2, mockk()),
                    ),
                    null,
                ),
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name3", 3, expectedSprite),
                    ),
                    expectedSprite,
                ),
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name3", 2, expectedSprite),
                    ),
                    expectedSprite,
                ),
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name3", 4, expectedSprite),
                    ),
                    expectedSprite,
                ),
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name1", 3, mockk()),
                        SizeSpecificSpriteInfo("name2", 3, mockk()),
                        SizeSpecificSpriteInfo("name3", 3, expectedSprite),
                        SizeSpecificSpriteInfo("name4", 3, mockk()),
                    ),
                    expectedSprite,
                ),
                // #10
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name3", 2, mockk()),
                        SizeSpecificSpriteInfo("name3", 4, mockk()),
                        SizeSpecificSpriteInfo("name3", 3, expectedSprite),
                    ),
                    expectedSprite,
                ),
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name1", 2, mockk()),
                        SizeSpecificSpriteInfo("name1", 4, mockk()),
                        SizeSpecificSpriteInfo("name1", 3, mockk()),
                        SizeSpecificSpriteInfo("name2", 2, mockk()),
                        SizeSpecificSpriteInfo("name2", 4, mockk()),
                        SizeSpecificSpriteInfo("name2", 3, mockk()),
                        SizeSpecificSpriteInfo("name3", 2, mockk()),
                        SizeSpecificSpriteInfo("name3", 4, mockk()),
                        SizeSpecificSpriteInfo("name3", 3, expectedSprite),
                        SizeSpecificSpriteInfo("name4", 2, mockk()),
                        SizeSpecificSpriteInfo("name4", 4, mockk()),
                        SizeSpecificSpriteInfo("name4", 3, mockk()),
                    ),
                    expectedSprite,
                ),
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name2", 3, mockk()),
                        SizeSpecificSpriteInfo("name4", 3, mockk()),
                        SizeSpecificSpriteInfo("name3", 2, expectedSprite),
                    ),
                    expectedSprite,
                ),
                arrayOf(
                    "name3",
                    "123",
                    listOf(
                        SizeSpecificSpriteInfo("name2", 3, mockk()),
                        SizeSpecificSpriteInfo("name4", 3, mockk()),
                        SizeSpecificSpriteInfo("name3", 4, expectedSprite),
                    ),
                    expectedSprite,
                ),
            )
        }
    }

    @Test
    fun getSpriteFrom() {
        val shieldToDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { mapboxShield } returns mockk {
                every { name() } returns shieldToDownloadName
                every { displayRef() } returns shieldToDownloadDisplayRef
            }
        }
        assertEquals(expected, shieldToDownload.getSpriteFrom(input))
    }
}
