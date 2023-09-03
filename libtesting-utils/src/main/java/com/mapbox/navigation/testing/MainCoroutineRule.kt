package com.mapbox.navigation.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
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
            Dispatchers.setMain(testDispatcher)

            try {
                base.evaluate()
            } finally {
                Dispatchers.resetMain() // Restore original main dispatcher
                createdScopes.forEach { it.cleanupTestCoroutines() }
            }

        }
    }

    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
        coroutineScope.runBlockingTest { block() }
}
