package com.mapbox.navigation.testing

import android.os.Looper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    fun createTestScope(): TestCoroutineScope {
        return TestCoroutineScope(testDispatcher + SupervisorJob()).also {
            createdScopes.add(it)
        }
    }

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            Dispatchers.setMain(testDispatcher)
            mockkStatic(Looper::class) {
                val looperMock = mockk<Looper>(relaxed = true)
                every { Looper.myLooper() } returns looperMock
                every { Looper.getMainLooper() } returns looperMock
                base.evaluate()
            }

            Dispatchers.resetMain() // Restore original main dispatcher
            createdScopes.forEach { it.cleanupTestCoroutines() }
        }
    }

    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
        coroutineScope.runBlockingTest { block() }
}
