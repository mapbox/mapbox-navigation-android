package com.mapbox.navigation.ui.shield.api

import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.shield.RoadShieldDownloader
import com.mapbox.navigation.ui.shield.RouteShieldProcessor
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
class MapboxRouteShieldApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val accessToken = "pk.123"
    private val routeShieldApi = MapboxRouteShieldApi(accessToken = accessToken)

    @Before
    fun setUp() {
        mockkObject(RouteShieldProcessor)
        mockkObject(RoadShieldDownloader)
    }

    @After
    fun tearDown() {
        unmockkObject(RouteShieldProcessor)
        unmockkObject(RoadShieldDownloader)
    }

    /*@Test
    fun `when request sprite and incorrect result`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val action = RouteShieldAction.GenerateSpriteUrl(
            userId = userId,
            styleId = styleId,
            accessToken = accessToken
        )
        val result = RouteShieldResult.ShieldPlaceholder.UnAvailable
        every {
            RouteShieldProcessor.process(action)
        } returns result
        val messageSlot = slot<Expected<RouteSpriteError, RouteSprite>>()

        routeShieldApi.requestSprite(
            userId = userId,
            styleId = styleId,
            consumer = spriteConsumer
        )

        verify(exactly = 1) { spriteConsumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $result emitted for $action.",
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when request sprite and download failure`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val url = "https://test.mapbox.com/$userId/$styleId/sprite.json?accessToken=$accessToken"
        val action = RouteShieldAction.GenerateSpriteUrl(
            userId = userId,
            styleId = styleId,
            accessToken = accessToken
        )
        val result = RouteShieldResult.OnSpriteUrl(url)
        every { RouteShieldProcessor.process(action) } returns result
        val errorMessage = "Resource is missing"
        coEvery {
            RoadShieldDownloader.downloadImage(url)
        } returns ExpectedFactory.createError(errorMessage)
        val messageSlot = slot<Expected<RouteSpriteError, RouteSprite>>()

        routeShieldApi.requestSprite(
            userId = userId,
            styleId = styleId,
            consumer = spriteConsumer
        )

        verify(exactly = 1) { spriteConsumer.accept(capture(messageSlot)) }
        assertEquals(
            errorMessage,
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when request sprite and download success parse json incorrect result`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val url = "https://test.mapbox.com/$userId/$styleId/sprite.json?accessToken=$accessToken"
        val action = RouteShieldAction.GenerateSpriteUrl(
            userId = userId,
            styleId = styleId,
            accessToken = accessToken
        )
        val result = RouteShieldResult.OnSpriteUrl(url)
        every { RouteShieldProcessor.process(action) } returns result
        val json = loadJsonFixture("invalid-sprite.json")
        coEvery {
            RoadShieldDownloader.downloadImage(url)
        } returns ExpectedFactory.createValue(json.toByteArray())
        val parseSpriteAction = RouteShieldAction.ParseSprite(json)
        val parseSpriteResult = RouteShieldResult.ShieldPlaceholder.UnAvailable
        every {
            RouteShieldProcessor.process(parseSpriteAction)
        } returns parseSpriteResult
        val errorMessage = "Inappropriate $parseSpriteResult emitted for $parseSpriteAction."
        val messageSlot = slot<Expected<RouteSpriteError, RouteSprite>>()

        routeShieldApi.requestSprite(
            userId = userId,
            styleId = styleId,
            consumer = spriteConsumer
        )

        verify(exactly = 1) { spriteConsumer.accept(capture(messageSlot)) }
        assertEquals(
            errorMessage,
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when request sprite and download success parse invalid json`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val url = "https://test.mapbox.com/$userId/$styleId/sprite.json?accessToken=$accessToken"
        val action = RouteShieldAction.GenerateSpriteUrl(
            userId = userId,
            styleId = styleId,
            accessToken = accessToken
        )
        val result = RouteShieldResult.OnSpriteUrl(url)
        every { RouteShieldProcessor.process(action) } returns result
        val json = loadJsonFixture("invalid-sprite.json")
        coEvery {
            RoadShieldDownloader.downloadImage(url)
        } returns ExpectedFactory.createValue(json.toByteArray())
        val errorMessage = "Error in parsing json: $json"
        val parseSpriteAction = RouteShieldAction.ParseSprite(json)
        val parseSpriteResult = RouteShieldResult.GenerateSprite.Failure(errorMessage)
        every {
            RouteShieldProcessor.process(parseSpriteAction)
        } returns parseSpriteResult
        val messageSlot = slot<Expected<RouteSpriteError, RouteSprite>>()

        routeShieldApi.requestSprite(
            userId = userId,
            styleId = styleId,
            consumer = spriteConsumer
        )

        verify(exactly = 1) { spriteConsumer.accept(capture(messageSlot)) }
        assertEquals(
            errorMessage,
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when request sprite and download success parse valid json`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val url = "https://test.mapbox.com/$userId/$styleId/sprite.json?accessToken=$accessToken"
        val action = RouteShieldAction.GenerateSpriteUrl(
            userId = userId,
            styleId = styleId,
            accessToken = accessToken
        )
        val result = RouteShieldResult.OnSpriteUrl(url)
        every { RouteShieldProcessor.process(action) } returns result
        val json = loadJsonFixture("valid-sprite.json")
        val shieldSprites = ShieldSprites.fromJson(json)
        coEvery {
            RoadShieldDownloader.downloadImage(url)
        } returns ExpectedFactory.createValue(json.toByteArray())
        val parseSpriteAction = RouteShieldAction.ParseSprite(json)
        val parseSpriteResult = RouteShieldResult.GenerateSprite.Success(shieldSprites)
        every {
            RouteShieldProcessor.process(parseSpriteAction)
        } returns parseSpriteResult
        val messageSlot = slot<Expected<RouteSpriteError, RouteSprite>>()

        routeShieldApi.requestSprite(
            userId = userId,
            styleId = styleId,
            consumer = spriteConsumer
        )

        verify(exactly = 1) { spriteConsumer.accept(capture(messageSlot)) }
        assertEquals(
            shieldSprites,
            messageSlot.captured.value!!.sprites
        )
    }

    @Test
    fun `when mapbox shield and imageBaseUrl is null incorrect default result`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = null
        val imageBaseUrl = null
        val options = MapboxRouteShieldOptions.Builder().build()
        val action = RouteShieldAction.GenerateDefaultShield(options.shieldSvg)
        val result = RouteShieldResult.GenerateSprite.Failure("whatever")
        every {
            RouteShieldProcessor.process(action)
        } returns result
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $result emitted for $action.",
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when mapbox shield and imageBaseUrl is null return default shield`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = null
        val imageBaseUrl = null
        val options = MapboxRouteShieldOptions.Builder().build()
        val action = RouteShieldAction.GenerateDefaultShield(options.shieldSvg)
        val shield = options.shieldSvg.toByteArray()
        val result = RouteShieldResult.OnBlankShield.Success(shield)
        every {
            RouteShieldProcessor.process(action)
        } returns result
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            shield,
            messageSlot.captured.value!!.shield
        )
        assertNull(messageSlot.captured.value!!.url)
        assertEquals(
            options.spriteAttributes,
            messageSlot.captured.value!!.sprite?.spriteAttributes()
        )
    }

    @Test
    fun `when userId and imageBaseUrl is null incorrect default result`() {
        val userId = null
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate-truck", "1")
        val imageBaseUrl = null
        val options = MapboxRouteShieldOptions.Builder().build()
        val action = RouteShieldAction.GenerateDefaultShield(options.shieldSvg)
        val result = RouteShieldResult.GenerateSprite.Failure("whatever")
        every {
            RouteShieldProcessor.process(action)
        } returns result
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $result emitted for $action.",
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when styleId and imageBaseUrl is null incorrect default result`() {
        val userId = "mockUserId"
        val styleId = null
        val mapboxShield = getMapboxShield("us-interstate-truck", "1")
        val imageBaseUrl = null
        val options = MapboxRouteShieldOptions.Builder().build()
        val action = RouteShieldAction.GenerateDefaultShield(options.shieldSvg)
        val result = RouteShieldResult.GenerateSprite.Failure("whatever")
        every {
            RouteShieldProcessor.process(action)
        } returns result
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $result emitted for $action.",
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when generate shield using imageBaseUrl failure to download shield`() {
        val imageBaseUrl = "https://test.mapbox.com/1234"
        val errorMessage = "Resource is missing."
        coEvery {
            RoadShieldDownloader.downloadImage(imageBaseUrl, true)
        } returns ExpectedFactory.createError(errorMessage)
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            errorMessage,
            messageSlot.captured.error!!.errorMessage
        )
        assertEquals(
            imageBaseUrl,
            messageSlot.captured.error!!.url
        )
    }

    @Test
    fun `when generate shield using imageBaseUrl success to download shield`() {
        val imageBaseUrl = "https://test.mapbox.com/1234"
        val json = loadJsonFixture("valid-blank-shield.json")
        val parsedShield = ShieldSvg.fromJson(json).svg().toByteArray()
        coEvery {
            RoadShieldDownloader.downloadImage(imageBaseUrl, true)
        } returns ExpectedFactory.createValue(parsedShield)
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            parsedShield,
            messageSlot.captured.value!!.shield
        )
        assertEquals(
            imageBaseUrl,
            messageSlot.captured.value!!.url
        )
        assertNull(messageSlot.captured.value!!.sprite)
    }

    @Test
    fun `when generate shield using mapbox and incorrect result for given action`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate", "12")
        val action = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val result = RouteShieldResult.GenerateSprite.Failure("whatever")
        every {
            RouteShieldProcessor.process(action)
        } returns result
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $result emitted for $action.",
            messageSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `when generate shield placeholder not available and imageBaseUrl is null`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate", "12")
        val action = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val result = RouteShieldResult.ShieldPlaceholder.UnAvailable
        every {
            RouteShieldProcessor.process(action)
        } returns result
        val options = MapboxRouteShieldOptions.Builder().build()
        val defaultShieldAction = RouteShieldAction.GenerateDefaultShield(options.shieldSvg)
        val shield = options.shieldSvg.toByteArray()
        val defaultShieldResult = RouteShieldResult.OnBlankShield.Success(shield)
        every {
            RouteShieldProcessor.process(defaultShieldAction)
        } returns defaultShieldResult
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            shield,
            messageSlot.captured.value!!.shield
        )
        assertNull(messageSlot.captured.value!!.url)
        assertEquals(
            options.spriteAttributes,
            messageSlot.captured.value!!.sprite?.spriteAttributes()
        )
    }

    @Test
    fun `when generate shield placeholder not available and imageBaseUrl is not null`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate", "12")
        val imageBaseUrl = "https://test.mapbox.com/1234"
        val json = loadJsonFixture("valid-blank-shield.json")
        val parsedShield = ShieldSvg.fromJson(json).svg().toByteArray()
        val action = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val result = RouteShieldResult.ShieldPlaceholder.UnAvailable
        every {
            RouteShieldProcessor.process(action)
        } returns result
        coEvery {
            RoadShieldDownloader.downloadImage(imageBaseUrl, true)
        } returns ExpectedFactory.createValue(parsedShield)
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            parsedShield,
            messageSlot.captured.value!!.shield
        )
        assertEquals(
            imageBaseUrl,
            messageSlot.captured.value!!.url
        )
        assertNull(messageSlot.captured.value!!.sprite)
    }

    @Test
    fun `when generate shield placeholder available incorrect result for action`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate-truck", "12")
        val mockSprite = ShieldSprite
            .builder()
            .spriteName("${mapboxShield.name()}-${mapboxShield.displayRef().length}")
            .spriteAttributes(
                ShieldSpriteAttribute
                    .builder()
                    .width(60)
                    .height(120)
                    .x(0)
                    .y(138)
                    .pixelRatio(1)
                    .placeholder(listOf(0.0, 17.0, 20.0, 23.0))
                    .visible(true)
                    .build()
            )
            .build()
        val placeholderAction = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val placeholderResult = RouteShieldResult.ShieldPlaceholder.Available(mockSprite)
        every {
            RouteShieldProcessor.process(placeholderAction)
        } returns placeholderResult
        val generateShieldUrlAction = RouteShieldAction.GenerateShieldUrl(
            userId,
            styleId,
            accessToken,
            mapboxShield.baseUrl(),
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val generateShieldUrlResult = RouteShieldResult.GenerateSprite.Failure("whatever")
        every {
            RouteShieldProcessor.process(generateShieldUrlAction)
        } returns generateShieldUrlResult
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            userId, styleId, mapboxShield, consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $generateShieldUrlResult emitted for $generateShieldUrlAction.",
            messageSlot.captured.error!!.errorMessage
        )
        assertNull(messageSlot.captured.error!!.url)
    }

    @Test
    fun `when generate shield download mapbox route shield error and imageBaseUrl is null`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate-truck", "12")
        val mockSprite = ShieldSprite
            .builder()
            .spriteName("${mapboxShield.name()}-${mapboxShield.displayRef().length}")
            .spriteAttributes(
                ShieldSpriteAttribute
                    .builder()
                    .width(60)
                    .height(120)
                    .x(0)
                    .y(138)
                    .pixelRatio(1)
                    .placeholder(listOf(0.0, 17.0, 20.0, 23.0))
                    .visible(true)
                    .build()
            )
            .build()
        val placeholderAction = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val placeholderResult = RouteShieldResult.ShieldPlaceholder.Available(mockSprite)
        every {
            RouteShieldProcessor.process(placeholderAction)
        } returns placeholderResult
        val error = "Resource is missing"
        coEvery {
            RoadShieldDownloader.downloadImage(
                mapboxShield.baseUrl()
                    .plus(userId)
                    .plus("/$styleId")
                    .plus("/sprite")
                    .plus("/${mapboxShield.name()}")
                    .plus("-${mapboxShield.displayRef().length}")
                    .plus("?access_token=$accessToken")
            )
        } returns ExpectedFactory.createError(error)
        val options = MapboxRouteShieldOptions.Builder().build()
        val defaultShieldAction = RouteShieldAction.GenerateDefaultShield(options.shieldSvg)
        val shield = options.shieldSvg.toByteArray()
        val defaultShieldResult = RouteShieldResult.OnBlankShield.Success(shield)
        every {
            RouteShieldProcessor.process(defaultShieldAction)
        } returns defaultShieldResult
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            shield,
            messageSlot.captured.value!!.shield
        )
        assertNull(messageSlot.captured.value!!.url)
        assertEquals(
            options.spriteAttributes,
            messageSlot.captured.value!!.sprite?.spriteAttributes()
        )
    }

    @Test
    fun `when generate shield download mapbox route shield error and imageBaseUrl is not null`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate-truck", "12")
        val mockSprite = ShieldSprite
            .builder()
            .spriteName("${mapboxShield.name()}-${mapboxShield.displayRef().length}")
            .spriteAttributes(
                ShieldSpriteAttribute
                    .builder()
                    .width(60)
                    .height(120)
                    .x(0)
                    .y(138)
                    .pixelRatio(1)
                    .placeholder(listOf(0.0, 17.0, 20.0, 23.0))
                    .visible(true)
                    .build()
            )
            .build()
        val placeholderAction = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val placeholderResult = RouteShieldResult.ShieldPlaceholder.Available(mockSprite)
        every {
            RouteShieldProcessor.process(placeholderAction)
        } returns placeholderResult
        val error = "Resource is missing"
        coEvery {
            RoadShieldDownloader.downloadImage(
                mapboxShield.baseUrl()
                    .plus(userId)
                    .plus("/$styleId")
                    .plus("/sprite")
                    .plus("/${mapboxShield.name()}")
                    .plus("-${mapboxShield.displayRef().length}")
                    .plus("?access_token=$accessToken")
            )
        } returns ExpectedFactory.createError(error)
        val imageBaseUrl = "https://test.mapbox.com/1234"
        val json = loadJsonFixture("valid-blank-shield.json")
        val parsedShield = ShieldSvg.fromJson(json).svg().toByteArray()
        coEvery {
            RoadShieldDownloader.downloadImage(imageBaseUrl, true)
        } returns ExpectedFactory.createValue(parsedShield)
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            imageBaseUrl = imageBaseUrl,
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            parsedShield,
            messageSlot.captured.value!!.shield
        )
        assertEquals(
            imageBaseUrl,
            messageSlot.captured.value!!.url
        )
        assertNull(messageSlot.captured.value!!.sprite)
    }

    @Test
    fun `when generate shield placeholder download route shield success incorrect result`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate-truck", "12")
        val mockSprite = ShieldSprite
            .builder()
            .spriteName("${mapboxShield.name()}-${mapboxShield.displayRef().length}")
            .spriteAttributes(
                ShieldSpriteAttribute
                    .builder()
                    .width(60)
                    .height(120)
                    .x(0)
                    .y(138)
                    .pixelRatio(1)
                    .placeholder(listOf(0.0, 17.0, 20.0, 23.0))
                    .visible(true)
                    .build()
            )
            .build()
        val placeholderAction = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val placeholderResult = RouteShieldResult.ShieldPlaceholder.Available(mockSprite)
        every {
            RouteShieldProcessor.process(placeholderAction)
        } returns placeholderResult
        val shield = loadJsonFixture("valid-blank-shield.json")
        val requestUrl = mapboxShield.baseUrl()
            .plus(userId)
            .plus("/$styleId")
            .plus("/sprite")
            .plus("/${mapboxShield.name()}")
            .plus("-${mapboxShield.displayRef().length}")
            .plus("?access_token=$accessToken")
        coEvery {
            RoadShieldDownloader.downloadImage(requestUrl)
        } returns ExpectedFactory.createValue(shield.toByteArray())
        val parseShieldAction = RouteShieldAction.ParseBlankShield(shield)
        val parseShieldResult = RouteShieldResult.GenerateSprite.Failure("whatever")
        every {
            RouteShieldProcessor.process(parseShieldAction)
        } returns parseShieldResult
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $parseShieldResult emitted for $parseShieldAction.",
            messageSlot.captured.error!!.errorMessage
        )
        assertEquals(
            requestUrl,
            messageSlot.captured.error!!.url
        )
    }

    @Test
    fun `when generate shield placeholder download route shield success incorrect parsing`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate-truck", "12")
        val mockSprite = ShieldSprite
            .builder()
            .spriteName("${mapboxShield.name()}-${mapboxShield.displayRef().length}")
            .spriteAttributes(
                ShieldSpriteAttribute
                    .builder()
                    .width(60)
                    .height(120)
                    .x(0)
                    .y(138)
                    .pixelRatio(1)
                    .placeholder(listOf(0.0, 17.0, 20.0, 23.0))
                    .visible(true)
                    .build()
            )
            .build()
        val placeholderAction = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val placeholderResult = RouteShieldResult.ShieldPlaceholder.Available(mockSprite)
        every {
            RouteShieldProcessor.process(placeholderAction)
        } returns placeholderResult
        val json = loadJsonFixture("invalid-blank-shield.json")
        val requestUrl = mapboxShield.baseUrl()
            .plus(userId)
            .plus("/$styleId")
            .plus("/sprite")
            .plus("/${mapboxShield.name()}")
            .plus("-${mapboxShield.displayRef().length}")
            .plus("?access_token=$accessToken")
        coEvery {
            RoadShieldDownloader.downloadImage(requestUrl)
        } returns ExpectedFactory.createValue(json.toByteArray())
        val errorMessage = "Error in parsing json: $json"
        val parseShieldAction = RouteShieldAction.ParseBlankShield(json)
        val parseShieldResult = RouteShieldResult.OnBlankShield.Failure(errorMessage)
        every {
            RouteShieldProcessor.process(parseShieldAction)
        } returns parseShieldResult
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            errorMessage,
            messageSlot.captured.error!!.errorMessage
        )
        assertEquals(
            requestUrl,
            messageSlot.captured.error!!.url
        )
    }

    @Test
    fun `when generate shield placeholder download route shield success correct result`() {
        val userId = "mockUserId"
        val styleId = "mockStyleId"
        val mapboxShield = getMapboxShield("us-interstate-truck", "12")
        val mockSprite = ShieldSprite
            .builder()
            .spriteName("${mapboxShield.name()}-${mapboxShield.displayRef().length}")
            .spriteAttributes(
                ShieldSpriteAttribute
                    .builder()
                    .width(60)
                    .height(120)
                    .x(0)
                    .y(138)
                    .pixelRatio(1)
                    .placeholder(listOf(0.0, 17.0, 20.0, 23.0))
                    .visible(true)
                    .build()
            )
            .build()
        val placeholderAction = RouteShieldAction.ShieldPlaceholderAvailable(
            mapboxShield.name(),
            mapboxShield.displayRef()
        )
        val placeholderResult = RouteShieldResult.ShieldPlaceholder.Available(mockSprite)
        every {
            RouteShieldProcessor.process(placeholderAction)
        } returns placeholderResult
        val json = loadJsonFixture("valid-blank-shield.json")
        val parsedShield = ShieldSvg.fromJson(json).svg().toByteArray()
        val requestUrl = mapboxShield.baseUrl()
            .plus(userId)
            .plus("/$styleId")
            .plus("/sprite")
            .plus("/${mapboxShield.name()}")
            .plus("-${mapboxShield.displayRef().length}")
            .plus("?access_token=$accessToken")
        coEvery {
            RoadShieldDownloader.downloadImage(requestUrl)
        } returns ExpectedFactory.createValue(json.toByteArray())
        val parseShieldAction = RouteShieldAction.ParseBlankShield(json)
        val parseShieldResult = RouteShieldResult.OnBlankShield.Success(parsedShield)
        every {
            RouteShieldProcessor.process(parseShieldAction)
        } returns parseShieldResult
        val messageSlot = slot<Expected<RouteShieldError, RouteShield>>()

        routeShieldApi.generateShield(
            userId = userId,
            styleId = styleId,
            mapboxShield = mapboxShield,
            consumer = consumer
        )

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(requestUrl, messageSlot.captured.value!!.url)
        assertEquals(mockSprite, messageSlot.captured.value!!.sprite)
        assertEquals(parsedShield, messageSlot.captured.value!!.shield)
    }

    private fun getMapboxShield(name: String, displayRef: String): MapboxShield {
        val baseUrl = "https://test.mapbox.com/"
        val textColor = "BLACK"
        return MapboxShield
            .builder()
            .name(name)
            .baseUrl(baseUrl)
            .textColor(textColor)
            .displayRef(displayRef)
            .build()
    }*/
}
