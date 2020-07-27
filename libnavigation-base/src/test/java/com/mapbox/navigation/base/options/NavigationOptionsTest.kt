package com.mapbox.navigation.base.options

import android.content.Context
import android.text.SpannableString
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.base.formatter.DistanceFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.javaType

class NavigationOptionsTest {

    val context: Context = mockk()

    val implClass = NavigationOptions::class
    val builderClass = implClass.nestedClasses.find { it.simpleName == "Builder" }!!

    @Before
    fun setup() {
        every { context.applicationContext } returns context

        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(context) } returns mockk()
    }

    @After
    fun teardown() {
        unmockkStatic(LocationEngineProvider::class)
    }

    @Test
    fun isNotDataClass() {
        Assert.assertFalse(implClass.isData)
        Assert.assertFalse(builderClass.isData)
    }

    @Test
    fun impl_onlyOneConstructor() {
        Assert.assertEquals(1, implClass.constructors.size)
    }

    @Test
    fun builder_onlyOneConstructor() {
        Assert.assertEquals(1, builderClass.constructors.size)
    }

    @Test
    fun impl_allConstructorsArePrivate() {
        Assert.assertTrue(implClass.constructors.all { it.visibility == KVisibility.PRIVATE })
    }

    @Test
    fun impl_allFieldsAreVals() {
        Assert.assertTrue(implClass.members.filterIsInstance<KProperty<*>>().all { it !is KMutableProperty })
    }

    @Test
    fun builder_allConstructorsArePrivate() {
        Assert.assertTrue(builderClass.constructors.all { it.visibility == KVisibility.PRIVATE })
    }

    @Test
    fun builder_hasNoPublicFields() {
        val publicFields = builderClass.members.filter { it is KProperty<*> && it.visibility != KVisibility.PRIVATE }
        Assert.assertEquals("there should be no public fields", 0, publicFields.size)
    }

    @Test
    fun equals() {
        val requiredFieldNames = builderClass.members.filter { it is KProperty && it !is KMutableProperty }.map { it.name }
        val requiredFieldTypes = builderClass.members.filter { it is KProperty && it !is KMutableProperty }.map { it.returnType }
        val requiredArgumentInstances = getOptionalArgumentInstances()
        requiredFieldTypes.forEachIndexed { index, kType ->
            Assert.assertEquals("required argument $index is incorrect", kType.javaType.typeName, requiredArgumentInstances[index]::class.java.simpleName)
        }

        val optionalFieldNames = builderClass.members.filter { it is KProperty && it is KMutableProperty }.map { it.name }
        val optionalFieldTypes = builderClass.members.filter { it is KProperty && it is KMutableProperty }.map { it.returnType }
        val optionalArgumentInstances = getOptionalArgumentInstances()
        optionalFieldTypes.forEachIndexed { index, kType ->
            Assert.assertEquals("optional argument $index is incorrect", kType.javaType.typeName, optionalArgumentInstances[index]::class.java.simpleName)
        }
    }

    private fun getOptionalArgumentInstances() : List<Any> {
        return listOf(mockk<Context>())
    }

    private fun getOptionalRequiredArgumentInstances() : List<Any> {
        return emptyList()
    }

    @Test
    fun whenBuilderBuildWithNoValuesCalledThenDefaultValuesUsed() {
        val options = NavigationOptions.Builder(context).build()

        assertEquals(options.timeFormatType, NONE_SPECIFIED)
        assertEquals(options.navigatorPredictionMillis, DEFAULT_NAVIGATOR_PREDICTION_MILLIS)
        assertEquals(options.distanceFormatter, null)
        assertNotNull(options.onboardRouterOptions)
    }

    @Test
    fun whenBuilderBuildCalledThenProperNavigationOptionsCreated() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }

        val options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .distanceFormatter(distanceFormatter)
            .build()

        assertEquals(options.timeFormatType, timeFormat)
        assertEquals(options.navigatorPredictionMillis, navigatorPredictionMillis)
        assertEquals(options.distanceFormatter, distanceFormatter)
    }

    @Test
    fun whenOptionsValuesChangedThenAllOtherValuesSaved() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }

        var options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .distanceFormatter(distanceFormatter)
            .build()

        val builder = options.toBuilder()
        val newTimeFormat = TWENTY_FOUR_HOURS
        val newNavigatorPredictionMillis = 900L
        options = builder
            .timeFormatType(newTimeFormat)
            .navigatorPredictionMillis(newNavigatorPredictionMillis)
            .build()

        assertEquals(options.timeFormatType, newTimeFormat)
        assertEquals(options.navigatorPredictionMillis, newNavigatorPredictionMillis)
        assertEquals(options.distanceFormatter, distanceFormatter)
    }

    @Test
    fun whenSeparateBuildersBuildSameOptions() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L

        val options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        val otherOptions = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        assertEquals(options, otherOptions)
    }

    @Test
    fun reuseChangedBuilder() {
        val builder = NavigationOptions.Builder(context)
        val options = builder.build()
        builder.accessToken("pk.123")

        assertNotEquals(options.toBuilder().build(), builder.build())
        assertEquals(options.toBuilder().build(), options)
    }
}
