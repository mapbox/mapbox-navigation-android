package com.mapbox.navigation.instrumentation_tests

{{PASTE IMPORTS HERE}}
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
{{PASTE TEST CLASSES HERE}}
)
class UiTestSuite
