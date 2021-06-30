package com.mapbox.navigation.base.internal.compatibility

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag

private val TAG = Tag("MbxFeatureCompatibility")

internal fun <T, V : FeatureCompatibilityTesters<T>> T.verifyCompatibility(
    logger: Logger,
    testers: V,
    testersConfiguration: V.() -> Unit
) {
    testers.apply(testersConfiguration)
    testers.getAllTesters().filter { it.enabled }.forEach { tester ->
        val messageBuilder = StringBuilder()
        messageBuilder.append(tester.initialMessage)
        messageBuilder.append(" Reasons:")
        var report = false
        tester.compatibilityChecks.forEach {
            if (!it.check(this)) {
                messageBuilder.append("\n")
                messageBuilder.append(it.messageIfFalse)
                report = true
            }
        }
        val message = messageBuilder.toString()
        if (report) {
            when (tester.severity) {
                FeatureCompatibilityTester.Severity.Exception -> throw RuntimeException(message)
                FeatureCompatibilityTester.Severity.Error -> logger.e(
                    TAG,
                    Message(message)
                )
                FeatureCompatibilityTester.Severity.Warning -> logger.w(
                    TAG,
                    Message(message)
                )
            }
        }
    }
}

class FeatureCompatibilityTester<T> internal constructor(
    val initialMessage: String,
    internal val severity: Severity,
    internal val compatibilityChecks: List<CompatibilityCheck<T>>
) {
    var enabled: Boolean = false

    internal enum class Severity {
        Exception,
        Error,
        Warning
    }
}

sealed class FeatureCompatibilityTesters<T> {
    internal abstract fun getAllTesters(): List<FeatureCompatibilityTester<T>>
}

internal class CompatibilityCheck<T>(val messageIfFalse: String, val check: (T) -> Boolean)
