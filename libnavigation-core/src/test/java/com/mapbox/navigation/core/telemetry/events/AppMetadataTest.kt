package com.mapbox.navigation.core.telemetry.events

import com.mapbox.navigation.testing.BuilderTest
import kotlin.reflect.KClass
import org.junit.Test

class AppMetadataTest : BuilderTest<AppMetadata, AppMetadata.Builder>() {
    override fun getImplementationClass(): KClass<AppMetadata> = AppMetadata::class

    override fun getFilledUpBuilder(): AppMetadata.Builder {
        return AppMetadata.Builder("name", "version")
            .sessionId("sessionId")
            .userId("userId")
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
