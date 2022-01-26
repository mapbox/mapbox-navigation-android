package com.mapbox.navigation.core.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import org.junit.Test
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import java.io.File

class RouteOptionsDetectorTest {
    @Test
    fun `no errors when default navigation options is applied`() {
        lint().files(
            kotlin(
                """
                package test.pkg;
                import com.mapbox.api.directions.v5.models.RouteOptions
                fun buildRouteOptions() {
                    RouteOptions.builder()
                        .applyDefaultNavigationOptions()
                        .build()
                }
                """
            ).indented(),
            mapboxDirectionsModelsFile()
        )
            .issues(RouteOptionsDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `error when default navigation is not applied`() {
        lint().files(
            kotlin(
                """
                package test.pkg;
                import com.mapbox.api.directions.v5.models.RouteOptions
                fun buildRouteOptions() {
                    RouteOptions.builder()
                        .build()
                }
                """
            ).indented(),
            mapboxDirectionsModelsFile()
        )
            .issues(RouteOptionsDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
    }
}

private fun mapboxDirectionsModelsFile(): TestFile {
    val path = System.getProperty("java.class.path")
        .split(':')
        .first { it.contains("mapbox-sdk-directions-models") }
    return TestFiles.LibraryReferenceTestFile(File(path))
}
