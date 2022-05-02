package com.mapbox.androidauto.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@ExperimentalCoroutinesApi
class MainCoroutineRule : TestRule {

    val testDispatcher = UnconfinedTestDispatcher()

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            Dispatchers.setMain(testDispatcher)

            base.evaluate()

            Dispatchers.resetMain() // Restore original main dispatcher
        }
    }

    fun runTest(block: suspend TestScope.() -> Unit) =
        runTest(testDispatcher, testBody = block)
}
