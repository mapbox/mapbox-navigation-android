package com.mapbox.navigation.core.replay.history

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MemoryTestRule : TestRule {

    private var startMemoryBytes: Long? = null

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            val runtime = Runtime.getRuntime()
            runtime.gc()
            startMemoryBytes = runtime.totalMemory() - runtime.freeMemory()

            base.evaluate()
        }
    }

    val memoryUsedMB: Double
        get() {
            val runtime = Runtime.getRuntime()
            val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
            return (memoryAfter - startMemoryBytes!!) * 1e-6
        }
}
