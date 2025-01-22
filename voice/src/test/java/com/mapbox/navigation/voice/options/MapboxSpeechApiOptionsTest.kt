package com.mapbox.navigation.voice.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class MapboxSpeechApiOptionsTest :
    BuilderTest<MapboxSpeechApiOptions, MapboxSpeechApiOptions.Builder>() {

    override fun getImplementationClass(): KClass<MapboxSpeechApiOptions> =
        MapboxSpeechApiOptions::class

    override fun getFilledUpBuilder(): MapboxSpeechApiOptions.Builder =
        MapboxSpeechApiOptions.Builder()
            .baseUri("https://api-routing-tiles-staging.tilestream.net")
            .gender(VoiceGender.MALE)

    @Test
    override fun trigger() {
        // read doc
    }
}
