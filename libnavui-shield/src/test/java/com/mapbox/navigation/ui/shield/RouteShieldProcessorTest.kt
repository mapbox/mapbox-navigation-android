package com.mapbox.navigation.ui.shield

import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.api.directions.v5.models.ShieldSvg
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteShieldProcessorTest {

    /*@Test
    fun `when sprite placeholder json is invalid then failure`() {
        val spriteJson = loadJsonFixture("invalid-sprite.json")
        val action = RouteShieldAction.ParseSprite(spriteJson)
        val expected = "Error in parsing json: $spriteJson"

        val actual = RouteShieldProcessor.process(action)

        assertTrue(actual is RouteShieldResult.GenerateSprite.Failure)
        assertEquals(
            expected,
            (actual as RouteShieldResult.GenerateSprite.Failure).error
        )
    }

    @Test
    fun `when sprite placeholder json is valid then success`() {
        val spriteJson = loadJsonFixture("valid-sprite.json")
        val shieldSprites = ShieldSprites.fromJson(spriteJson)
        val action = RouteShieldAction.ParseSprite(spriteJson)
        val expected = shieldSprites.sprites()

        val actual = RouteShieldProcessor.process(action)

        assertTrue(actual is RouteShieldResult.GenerateSprite.Success)
        assertEquals(
            expected,
            (actual as RouteShieldResult.GenerateSprite.Success).sprites.sprites()
        )
    }

    @Test
    fun `when shield name does not exist then failure`() {
        val spriteJson = loadJsonFixture("valid-sprite.json")
        val spriteAction = RouteShieldAction.ParseSprite(spriteJson)
        RouteShieldProcessor.process(spriteAction)
        val action = RouteShieldAction.ShieldPlaceholderAvailable(
            shieldName = "us-interstate",
            displayRef = "12"
        )

        val actual = RouteShieldProcessor.process(action)

        assertTrue(actual is RouteShieldResult.ShieldPlaceholder.UnAvailable)
    }

    @Test
    fun `when shield name does not have placeholder then failure`() {
        val spriteJson = loadJsonFixture("valid-sprite.json")
        val spriteAction = RouteShieldAction.ParseSprite(spriteJson)
        RouteShieldProcessor.process(spriteAction)
        val action = RouteShieldAction.ShieldPlaceholderAvailable(
            shieldName = "us-interstate-truck",
            displayRef = "12345"
        )

        val actual = RouteShieldProcessor.process(action)

        assertTrue(actual is RouteShieldResult.ShieldPlaceholder.UnAvailable)
    }

    @Test
    fun `when shield name has placeholder with display ref one then success`() {
        val spriteJson = loadJsonFixture("valid-sprite.json")
        val spriteAction = RouteShieldAction.ParseSprite(spriteJson)
        RouteShieldProcessor.process(spriteAction)
        val action = RouteShieldAction.ShieldPlaceholderAvailable(
            shieldName = "us-interstate-truck",
            displayRef = "1"
        )
        val expected = "us-interstate-truck-2"

        val actual =
            RouteShieldProcessor.process(action) as RouteShieldResult.ShieldPlaceholder.Available

        assertEquals(expected, actual.sprite.spriteName())
    }

    @Test
    fun `when shield name has placeholder with display ref two then success`() {
        val spriteJson = loadJsonFixture("valid-sprite.json")
        val spriteAction = RouteShieldAction.ParseSprite(spriteJson)
        RouteShieldProcessor.process(spriteAction)
        val action = RouteShieldAction.ShieldPlaceholderAvailable(
            shieldName = "us-interstate-truck",
            displayRef = "12"
        )
        val expected = "us-interstate-truck-2"

        val actual =
            RouteShieldProcessor.process(action) as RouteShieldResult.ShieldPlaceholder.Available

        assertEquals(expected, actual.sprite.spriteName())
    }

    @Test
    fun `when userid and styleid then generate shield url`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val accessToken = "mockAccessToken"
        val baseUrl = "https://test.mapbox.com/"
        val shieldName = "test-shield"
        val displayRef = "123"
        val action = RouteShieldAction.GenerateShieldUrl(
            userId,
            styleId,
            accessToken,
            baseUrl,
            shieldName,
            displayRef
        )
        val expected = baseUrl
            .plus(userId)
            .plus("/$styleId")
            .plus("/sprite")
            .plus("/$shieldName")
            .plus("-${displayRef.length}")
            .plus("?access_token=$accessToken")

        val actual = RouteShieldProcessor.process(action) as RouteShieldResult.OnShieldUrl

        assertEquals(expected, actual.shieldUrl)
    }

    @Test
    fun `when shield json invalid then parse blank shield failure`() {
        val json = loadJsonFixture("invalid-blank-shield.json")
        val action = RouteShieldAction.ParseBlankShield(json)

        val result = RouteShieldProcessor.process(action)

        assertTrue(result is RouteShieldResult.OnBlankShield.Failure)
    }

    @Test
    fun `when shield json valid then parse blank shield success`() {
        val json = loadJsonFixture("valid-blank-shield.json")
        val action = RouteShieldAction.ParseBlankShield(json)
        val expected = ShieldSvg.fromJson(json).svg().toByteArray()

        val actual = RouteShieldProcessor.process(action) as RouteShieldResult.OnBlankShield.Success

        assertArrayEquals(expected, actual.shield)
    }*/
}
