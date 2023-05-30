package com.mapbox.navigation.testing

import android.annotation.SuppressLint
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.testing.factories.TestSDKRouteParser
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class NativeRouteParserRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @SuppressLint("VisibleForTests")
            override fun evaluate() {
                SDKRouteParser.default = TestSDKRouteParser()
                try {
                    base.evaluate()
                } finally {
                    SDKRouteParser.resetDefault()
                }
            }
        }
    }
}
