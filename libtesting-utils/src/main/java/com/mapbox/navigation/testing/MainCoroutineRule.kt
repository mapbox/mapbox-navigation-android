package com.mapbox.navigation.testing

import com.mapbox.common.dispatchers.SdkDispatchers
import com.mapbox.common.dispatchers.resetTestMain
import com.mapbox.common.dispatchers.setTestMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.jvm.Throws

@ExperimentalCoroutinesApi
class MainCoroutineRule : TestRule {
    val testDispatcher = TestCoroutineDispatcher()
    val createdScopes = mutableListOf<TestCoroutineScope>()
    val coroutineScope = createTestScope()

    fun createTestScope(job: Job = SupervisorJob()): TestCoroutineScope {
        return TestCoroutineScope(testDispatcher + job).also {
            createdScopes.add(it)
        }
    }

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            SdkDispatchers.setTestMain(testDispatcher)

            try {
                base.evaluate()
            } finally {
                SdkDispatchers.resetTestMain()
                createdScopes.forEach { it.cleanupTestCoroutines() }
            }
        }
    }

    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
        coroutineScope.runTest { block() }
}
