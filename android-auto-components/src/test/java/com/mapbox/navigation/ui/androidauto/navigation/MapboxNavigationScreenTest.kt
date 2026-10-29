package com.mapbox.navigation.ui.androidauto.navigation

import android.content.Context
import android.text.SpannableString
import androidx.activity.OnBackPressedDispatcher
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.action.MapboxScreenActionStripProvider
import com.mapbox.navigation.ui.androidauto.action.buildSettingsAction
import com.mapbox.navigation.ui.androidauto.freedrive.FreeDriveActionStrip
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.settings.CarSettingsMode
import com.mapbox.navigation.ui.androidauto.settings.CarSettingsTemplateProvider
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.androidauto.testing.TestOnDoneCallback
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.voice.api.MapboxAudioGuidanceState
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class MapboxNavigationScreenTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val backDispatcher = OnBackPressedDispatcher()
    private val appManager: AppManager = mockk(relaxed = true)
    private val carMap: MapboxCarMap = mockk(relaxed = true)
    private val carContext: CarContext = mockk(relaxed = true) {
        every { getString(any()) } answers { context.getString(firstArg()) }
        every { resources } returns context.resources
        every { packageName } returns context.packageName
        every { carAppApiLevel } returns 7
        every { onBackPressedDispatcher } returns backDispatcher
        every { getCarService(AppManager::class.java) } returns appManager
        every { getCarService(ConstraintManager::class.java) } returns mockk {
            every { getContentLimit(any()) } returns 10
        }
    }
    private val mapboxCarContext: MapboxCarContext = mockk(relaxed = true) {
        every { carContext } returns this@MapboxNavigationScreenTest.carContext
        every { mapboxCarMap } returns carMap
        every { options } returns MapboxCarOptions()
        every { mapboxCarStorage.readSharedPref(any(), false) } returns false
    }
    private val routesObserver = slot<RoutesObserver>()
    private val previewObserver = slot<RoutesPreviewObserver>()
    private val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { registerRoutesObserver(capture(routesObserver)) } returns Unit
        every { registerRoutesPreviewObserver(capture(previewObserver)) } returns Unit
        every { getNavigationRoutes() } returns emptyList()
        every { getRoutesPreview() } returns null
    }
    private lateinit var screen: MapboxNavigationScreen
    private val navigationInfo = MutableStateFlow(CarNavigationInfo())

    @Before
    fun setUp() {
        mockkConstructor(CarNavigationInfoProvider::class)
        every { anyConstructed<CarNavigationInfoProvider>().carNavigationInfo } returns navigationInfo
        mockkObject(MapboxAudioGuidance.Companion)
        every { MapboxAudioGuidance.getRegisteredInstance() } returns mockk(relaxed = true) {
            every { stateFlow() } returns MutableStateFlow(
                mockk<MapboxAudioGuidanceState> {
                    every { isMuted } returns false
                    every { isPlayable } returns true
                },
            )
        }
        mockkObject(MapboxScreenManager.Companion)
        mockkStatic(MapboxScreenManager::class)
        every { MapboxScreenManager.push(any()) } returns Unit
        mockkStatic(CarDistanceFormatter::class)
        every { CarDistanceFormatter.formatDistance(any()) } returns SpannableString("1 km")
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        carAppTestRule.onAttached(mapboxNavigation)
        screen = MapboxNavigationScreen(mapboxCarContext)
        (screen.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.RESUMED
    }

    @After
    fun tearDown() {
        (screen.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.DESTROYED
    }

    @Test
    fun `free drive settings action shows settings alongside the attached map`() {
        val initialTemplate = screen.onGetTemplate() as NavigationTemplate

        click(initialTemplate.actionStrip!!.actions.first())

        val template = screen.onGetTemplate() as MapWithContentTemplate
        val settings = template.contentTemplate as ListTemplate
        assertEquals(context.getString(R.string.car_settings_title), settings.header!!.title.toString())
        assertEquals(Action.BACK, settings.header!!.startHeaderAction)
        assertNotNull(template.mapController!!.mapActionStrip)
        assertEquals(
            initialTemplate.actionStrip!!.actions.drop(1),
            template.actionStrip!!.actions,
        )
        verify(exactly = 0) { MapboxScreenManager.push(any()) }
        backDispatcher.onBackPressed()
        verify(exactly = 0) { carMap.unregisterObserver(any()) }
        verify(exactly = 0) { carMap.setGestureHandler(null) }
    }

    @Test
    fun `back closes settings before navigating back in free drive`() {
        click(buildSettingsAction(screen))

        backDispatcher.onBackPressed()

        assertTrue(screen.onGetTemplate() is NavigationTemplate)
        verify(exactly = 0) { mapboxCarContext.mapboxScreenManager.goBack() }
        backDispatcher.onBackPressed()
        verify(exactly = 1) { mapboxCarContext.mapboxScreenManager.goBack() }
    }

    @Test
    fun `back from preview settings preserves the preview until back is pressed again`() {
        emitPreview()
        val initialTemplate = screen.onGetTemplate() as MapWithContentTemplate
        click(initialTemplate.actionStrip!!.actions.first())
        assertEquals(
            context.getString(R.string.car_settings_title),
            settingsTemplate().header!!.title.toString(),
        )

        backDispatcher.onBackPressed()

        val restoredTemplate = screen.onGetTemplate() as MapWithContentTemplate
        assertEquals(
            context.getString(R.string.car_action_preview_title),
            (restoredTemplate.contentTemplate as ListTemplate).header!!.title.toString(),
        )
        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
        backDispatcher.onBackPressed()
        verify(exactly = 1) { mapboxNavigation.setRoutesPreview(emptyList()) }
    }

    @Test
    fun `guidance settings keeps guidance actions and back restores navigation`() {
        emitActiveRoutes(listOf(route()))
        val initialTemplate = screen.onGetTemplate() as NavigationTemplate
        assertEquals(4, initialTemplate.actionStrip!!.actions.size)

        click(initialTemplate.actionStrip!!.actions.first())

        val template = screen.onGetTemplate() as MapWithContentTemplate
        assertEquals(
            initialTemplate.actionStrip!!.actions.drop(1),
            template.actionStrip!!.actions,
        )
        assertEquals(
            context.getString(R.string.car_action_navigation_stop_button),
            template.actionStrip!!.actions.last().title.toString(),
        )
        backDispatcher.onBackPressed()
        assertTrue(screen.onGetTemplate() is NavigationTemplate)
        verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(any()) }
    }

    @Test
    fun `starting guidance closes settings without requiring back`() {
        emitPreview()
        click(buildSettingsAction(screen))

        emitActiveRoutes(listOf(route()))

        assertTrue(screen.onGetTemplate() is NavigationTemplate)
        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
        verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(any()) }
    }

    @Test
    fun `receiving a route preview closes free drive settings`() {
        click(buildSettingsAction(screen))

        emitPreview()

        assertEquals(
            context.getString(R.string.car_action_preview_title),
            settingsTemplate().header!!.title.toString(),
        )
        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
    }

    @Test
    fun `stopping guidance closes settings`() {
        emitActiveRoutes(listOf(route()))
        click(buildSettingsAction(screen))

        emitActiveRoutes(emptyList())

        assertTrue(screen.onGetTemplate() is NavigationTemplate)
    }

    @Test
    fun `settings stay closed after another screen is pushed and popped`() {
        click(buildSettingsAction(screen))
        val lifecycle = screen.lifecycle as LifecycleRegistry

        lifecycle.currentState = Lifecycle.State.CREATED
        emitPreview()
        lifecycle.currentState = Lifecycle.State.RESUMED

        assertEquals(
            context.getString(R.string.car_action_preview_title),
            settingsTemplate().header!!.title.toString(),
        )
    }

    @Test
    fun `pausing closes settings even when navigation mode does not change`() {
        click(buildSettingsAction(screen))
        val lifecycle = screen.lifecycle as LifecycleRegistry

        lifecycle.currentState = Lifecycle.State.STARTED
        lifecycle.currentState = Lifecycle.State.RESUMED

        assertTrue(screen.onGetTemplate() is NavigationTemplate)
    }

    @Test
    fun `route updates in the same mode keep settings open`() {
        emitActiveRoutes(listOf(route()))
        click(buildSettingsAction(screen))

        emitActiveRoutes(listOf(route()))

        assertEquals(
            context.getString(R.string.car_settings_title),
            settingsTemplate().header!!.title.toString(),
        )
    }

    @Test
    fun `navigation info updates do not invalidate settings and back shows the latest info`() {
        emitActiveRoutes(listOf(route()))
        click(buildSettingsAction(screen))
        clearMocks(appManager, answers = false)

        val latestInfo = RoutingInfo.Builder().setLoading(true).build()
        navigationInfo.value = CarNavigationInfo(navigationInfo = latestInfo)

        verify(exactly = 0) { appManager.invalidate() }
        backDispatcher.onBackPressed()
        assertEquals(latestInfo, (screen.onGetTemplate() as NavigationTemplate).navigationInfo)
        navigationInfo.value = CarNavigationInfo()
        verify(exactly = 2) { appManager.invalidate() }
    }

    @Test
    fun `route preview settings omit the default settings action`() {
        emitPreview()
        val initialTemplate = screen.onGetTemplate() as MapWithContentTemplate

        click(initialTemplate.actionStrip!!.actions.first())

        val template = screen.onGetTemplate() as MapWithContentTemplate
        assertEquals(initialTemplate.actionStrip!!.actions.drop(1), template.actionStrip!!.actions)
    }

    @Test
    fun `inline setting writes the shared preference and reads it when reopened`() {
        val key = context.getString(R.string.car_settings_toggle_place_holder_key)
        click(buildSettingsAction(screen))
        val row = settingsTemplate().singleList!!.items.single() as Row
        val callback = TestOnDoneCallback()

        row.toggle!!.onCheckedChangeDelegate!!.sendCheckedChange(true, callback)

        callback.assertSuccess()
        verify { mapboxCarContext.mapboxCarStorage.writeSharedPref(key, true) }
        every { mapboxCarContext.mapboxCarStorage.readSharedPref(key, false) } returns true
        backDispatcher.onBackPressed()
        click(buildSettingsAction(screen))
        assertTrue((settingsTemplate().singleList!!.items.single() as Row).toggle!!.isChecked)
    }

    @Test
    fun `settings action on legacy screens still pushes the dedicated screen`() {
        val legacyScreen = object : Screen(carContext) {
            override fun onGetTemplate() = NavigationTemplate.Builder().build()
        }

        click(buildSettingsAction(legacyScreen))

        verify(exactly = 1) { MapboxScreenManager.push(MapboxScreen.SETTINGS) }
    }

    @Test
    fun `legacy preview and guidance action strips do not gain a settings action`() {
        val legacyScreen = object : Screen(carContext) {
            override fun onGetTemplate() = NavigationTemplate.Builder().build()
        }
        val provider = MapboxScreenActionStripProvider()

        assertEquals(
            1,
            provider.getActionStrip(legacyScreen, MapboxScreen.ROUTE_PREVIEW).actions.size,
        )
        assertEquals(
            3,
            provider.getActionStrip(legacyScreen, MapboxScreen.ACTIVE_GUIDANCE).actions.size,
        )
    }

    @Test
    fun `settings action exposes three Java overloads`() {
        val actionClass = Class.forName(
            "com.mapbox.navigation.ui.androidauto.action.CarSettingsActionKt",
        )

        assertEquals(
            listOf(1, 2, 3),
            actionClass.declaredMethods
                .filter { it.name == "buildSettingsAction" }
                .map { it.parameterTypes.size }
                .sorted(),
        )
    }

    @Test
    fun `custom action strip remains in use when inline settings are shown`() {
        val provider = mockk<MapboxScreenActionStripProvider>()
        val actions = FreeDriveActionStrip(screen).builder().build()
        every { provider.getActionStrip(screen, MapboxScreen.FREE_DRIVE) } returns actions
        mapboxCarContext.options.applyCustomization(
            MapboxCarOptions.Customization().apply { actionsStripProvider = provider },
        )

        click(buildSettingsAction(screen))

        assertEquals(actions, (screen.onGetTemplate() as MapWithContentTemplate).actionStrip)
    }

    @Test
    fun `free drive settings compatibility method shows inline settings`() {
        click(FreeDriveActionStrip(screen).buildSettingsAction())

        assertEquals(
            context.getString(R.string.car_settings_title),
            settingsTemplate().header!!.title.toString(),
        )
        verify(exactly = 0) { MapboxScreenManager.push(any()) }
    }

    @Test
    fun `settings action uses the default settings icon`() {
        val action = buildSettingsAction(screen)

        assertEquals(R.drawable.ic_settings_24px, action.icon!!.icon!!.resId)
    }

    @Test
    fun `settings action supports a custom title with the default icon`() {
        val action = buildSettingsAction(screen, title = "Preferences")
        click(action)

        assertEquals("Preferences", action.title!!.toString())
        assertEquals(R.drawable.ic_settings_24px, action.icon!!.icon!!.resId)
        assertEquals(
            context.getString(R.string.car_settings_title),
            settingsTemplate().header!!.title.toString(),
        )
    }

    @Test
    fun `custom settings icon preserves inline settings behavior`() {
        val icon = CarIcon.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_search_24px),
        ).build()

        val action = buildSettingsAction(screen, icon, title = "Preferences")
        click(action)

        assertEquals(icon, action.icon)
        assertEquals("Preferences", action.title!!.toString())
        assertEquals(
            context.getString(R.string.car_settings_title),
            settingsTemplate().header!!.title.toString(),
        )
        verify(exactly = 0) { MapboxScreenManager.push(any()) }
    }

    @Test
    fun `custom settings icon preserves legacy settings behavior`() {
        val legacyScreen = object : Screen(carContext) {
            override fun onGetTemplate() = NavigationTemplate.Builder().build()
        }
        val icon = CarIcon.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_search_24px),
        ).build()

        val action = buildSettingsAction(legacyScreen, icon, title = "Preferences")
        click(action)

        assertEquals(icon, action.icon)
        assertEquals("Preferences", action.title!!.toString())
        verify(exactly = 1) { MapboxScreenManager.push(MapboxScreen.SETTINGS) }
    }

    @Test
    fun `settings provider receives the current mode when reopened after mode changes`() {
        val requestedModes = mutableListOf<CarSettingsMode>()
        mapboxCarContext.options.applyCustomization(
            MapboxCarOptions.Customization().apply {
                settingsTemplateProvider = CarSettingsTemplateProvider { owner, mode ->
                    assertEquals(screen, owner)
                    requestedModes.add(mode)
                    customSettingsTemplate(mode.name)
                }
            },
        )
        click(buildSettingsAction(screen))

        assertEquals(CarSettingsMode.FREE_DRIVE.name, settingsTemplate().header!!.title.toString())
        emitPreview()
        click(buildSettingsAction(screen))
        assertEquals(CarSettingsMode.ROUTE_PREVIEW.name, settingsTemplate().header!!.title.toString())
        emitActiveRoutes(listOf(route()))
        click(buildSettingsAction(screen))
        assertEquals(CarSettingsMode.ACTIVE_GUIDANCE.name, settingsTemplate().header!!.title.toString())
        assertEquals(
            listOf(
                CarSettingsMode.FREE_DRIVE,
                CarSettingsMode.ROUTE_PREVIEW,
                CarSettingsMode.ACTIVE_GUIDANCE,
            ),
            requestedModes,
        )

        backDispatcher.onBackPressed()
        assertTrue(screen.onGetTemplate() is NavigationTemplate)
        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
        verify(exactly = 0) { MapboxScreenManager.push(any()) }
    }

    @Test
    fun `settings content is rebuilt by the provider instead of the placeholder`() {
        var title = "Custom settings"
        mapboxCarContext.options.applyCustomization(
            MapboxCarOptions.Customization().apply {
                settingsTemplateProvider = CarSettingsTemplateProvider { _, _ ->
                    customSettingsTemplate(title)
                }
            },
        )
        click(buildSettingsAction(screen))
        assertEquals(title, (settingsTemplate().singleList!!.items.single() as Row).title.toString())

        title = "Updated settings"
        screen.invalidate()

        assertEquals(title, (settingsTemplate().singleList!!.items.single() as Row).title.toString())
        backDispatcher.onBackPressed()
        click(buildSettingsAction(screen))
        assertEquals(title, settingsTemplate().header!!.title.toString())
    }

    private fun customSettingsTemplate(title: String): ListTemplate = ListTemplate.Builder()
        .setHeader(
            Header.Builder().setTitle(title).setStartHeaderAction(Action.BACK).build(),
        )
        .setSingleList(ItemList.Builder().addItem(Row.Builder().setTitle(title).build()).build())
        .build()

    private fun settingsTemplate(): ListTemplate =
        (screen.onGetTemplate() as MapWithContentTemplate).contentTemplate as ListTemplate

    private fun click(action: Action) {
        val callback = TestOnDoneCallback()
        action.onClickDelegate!!.sendClick(callback)
        callback.assertSuccess()
    }

    private fun emitActiveRoutes(routes: List<NavigationRoute>) {
        routesObserver.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns routes },
        )
    }

    private fun emitPreview() {
        val routes = listOf(route())
        val preview: RoutesPreview = mockk {
            every { originalRoutesList } returns routes
            every { routesList } returns routes
            every { primaryRouteIndex } returns 0
            every { primaryRoute } returns routes.first()
        }
        previewObserver.captured.routesPreviewUpdated(
            mockk { every { routesPreview } returns preview },
        )
    }

    private fun route(): NavigationRoute = mockk {
        every { id } returns "route"
        every { directionsRoute } returns mockk {
            every { duration() } returns 60.0
            every { distance() } returns 1_000.0
            every { legs() } returns emptyList()
        }
    }
}
