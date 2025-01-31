package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class EventsAppMetadataTest : BuilderTest<EventsAppMetadata, EventsAppMetadata.Builder>() {
    override fun getImplementationClass(): KClass<EventsAppMetadata> = EventsAppMetadata::class

    override fun getFilledUpBuilder(): EventsAppMetadata.Builder {
        return EventsAppMetadata.Builder("name", "version")
            .userId("userId")
            .sessionId("sessionId")
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
