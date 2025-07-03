package com.mapbox.navigation.testing

import android.annotation.SuppressLint
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.testing.factories.TestSDKRouteParser
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class NativeRouteParserRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @SuppressLint("VisibleForTests")
            override fun evaluate() {
                mockkObject(SDKRouteParser)
                every { SDKRouteParser.default } returns TestSDKRouteParser()

                try {
                    base.evaluate()
                } finally {
                    unmockkObject(SDKRouteParser)
                }
            }
        }
    }
}
