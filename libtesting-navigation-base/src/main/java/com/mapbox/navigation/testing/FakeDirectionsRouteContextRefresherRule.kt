package com.mapbox.navigation.testing

import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.navigation.base.internal.route.DirectionsRouteContextRefresher
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Swaps [DirectionsRouteContextRefresher.default] for a fake that always succeeds without
 * touching the native peer, the same way [NativeRouteParserRule] swaps out native parsing.
 */
class FakeDirectionsRouteContextRefresherRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                mockkObject(DirectionsRouteContextRefresher)
                every { DirectionsRouteContextRefresher.default } returns
                    EchoingDirectionsRouteContextRefresher()

                try {
                    base.evaluate()
                } finally {
                    unmockkObject(DirectionsRouteContextRefresher)
                }
            }
        }
    }
}

private class EchoingDirectionsRouteContextRefresher : DirectionsRouteContextRefresher {
    override fun refresh(
        context: DirectionsRouteContext,
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
    ): Expected<String, DirectionsRouteContext> = ExpectedFactory.createValue(context)
}
