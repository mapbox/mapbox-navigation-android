package com.mapbox.navigation.core.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.evaluateString
import com.android.tools.lint.detector.api.Detector.UastScanner
import org.jetbrains.uast.UElement

class RouteOptionsDetector : Detector(), UastScanner {

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "RouteOptionsNotCompatibleWithSDK",
            briefDescription = "Route Options isn't compatible with SDK",
            explanation = """
                    Route Options isn't compatible with SDK
                    """,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                RouteOptionsDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(ULiteralExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitLiteralExpression(node: ULiteralExpression) {
                val string = node.evaluateString() ?: return
                if (string.contains("lint") && string.matches(Regex(".*\\blint\\b.*"))) {
                    context.report(
                        ISSUE, node, context.getLocation(node),
                        "This code mentions `lint`: **Congratulations**"
                    )
                }
            }
        }
    }
}
