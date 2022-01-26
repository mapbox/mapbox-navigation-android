package com.mapbox.navigation.core.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import org.junit.Test
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint

class RouteOptionsDetectorTest {
    @Test
    fun testBasic() {
        lint().files(
            kotlin(
                """
                package test.pkg;
                fun buildRouteOptions() {
                    val a = "lint"
                }
                """
            ).indented()
        )
            .issues(RouteOptionsDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
    }
}
