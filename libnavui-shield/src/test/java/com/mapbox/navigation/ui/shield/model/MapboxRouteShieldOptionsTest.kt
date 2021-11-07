package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.ShieldSpriteAttribute
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class MapboxRouteShieldOptionsTest : BuilderTest<MapboxRouteShieldOptions,
    MapboxRouteShieldOptions.Builder>() {

    override fun getImplementationClass(): KClass<MapboxRouteShieldOptions> =
        MapboxRouteShieldOptions::class

    override fun getFilledUpBuilder(): MapboxRouteShieldOptions.Builder {
        val shieldSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
            "id=\"rectangle-white-2\" " +
            "width=\"30\" " +
            "height=\"12\" " +
            "viewBox=\"0 0 10 4\">" +
            "<g>" +
            "<path d=\"M0,0 H20 V14 H0 Z\" fill=\"none\"/>" +
            "<path d=\"M3,1 H17 C17,1 19,1 19,3 V11 C19,11 19,13 17,13 H3 C3,13 1,13 1,11 V3 C1," +
            "3 1,1 3,1\" fill=\"none\" stroke=\"#1b1d27\" stroke-linejoin=\"round\" " +
            "stroke-miterlimit=\"4px\" stroke-width=\"2\"/>" +
            "<path d=\"M3,1 H17 C17,1 19,1 19,3 V11 C19,11 19,13 17,13 H3 C3,13 1,13 1,11 V3 C1," +
            "3 1,1 3,1\" fill=\"#ffffff\"/>" +
            "<path d=\"M0,4 H20 V10 H0 Z\" fill=\"none\" id=\"mapbox-text-placeholder\"/>" +
            "</g>" +
            "</svg>"
        val spriteAttributes = ShieldSpriteAttribute
            .builder()
            .width(30)
            .height(12)
            .x(252)
            .y(663)
            .pixelRatio(1)
            .placeholder(listOf(0.0, 1.0, 10.0, 3.0))
            .visible(true)
            .build()
        return MapboxRouteShieldOptions.Builder()
            .spriteAttributes(spriteAttributes)
            .shieldSvg(shieldSvg)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
