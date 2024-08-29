package com.mapbox.navigation.tripdata.shield

import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.navigation.tripdata.shield.internal.model.SizeSpecificSpriteInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ShieldsCacheToSizeSpecificSpriteInfosTest(
    private val sprites: List<ShieldSprite>,
    private val expected: List<SizeSpecificSpriteInfo>,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Array<Any>> {
            val spriteWithoutDash = mockk<ShieldSprite> {
                every { spriteName() } returns "name"
            }
            val spriteWithoutNumber = mockk<ShieldSprite> {
                every { spriteName() } returns "some-name"
            }
            val spriteWithNumber2 = mockk<ShieldSprite> {
                every { spriteName() } returns "name-2"
            }
            val spriteWithNumber3 = mockk<ShieldSprite> {
                every { spriteName() } returns "name-3"
            }
            val spriteWithNumberAndOtherDashes = mockk<ShieldSprite> {
                every { spriteName() } returns "some-name-1-4"
            }
            return listOf(
                arrayOf(emptyList<ShieldSprite>(), emptyList<SizeSpecificSpriteInfo>()),
                arrayOf(
                    listOf(
                        spriteWithNumber2,
                        spriteWithoutDash,
                        spriteWithNumber3,
                        spriteWithoutNumber,
                        spriteWithNumberAndOtherDashes,
                    ),
                    listOf(
                        SizeSpecificSpriteInfo("name", 2, spriteWithNumber2),
                        SizeSpecificSpriteInfo("name", 3, spriteWithNumber3),
                        SizeSpecificSpriteInfo("some-name-1", 4, spriteWithNumberAndOtherDashes),
                    ),
                ),
            )
        }
    }

    @Test
    fun toSizeSpecificSpriteInfos() {
        val actual = ShieldSprites.builder().sprites(sprites).build()
            .toSizeSpecificSpriteInfos()
        assertEquals(expected, actual)
    }
}
