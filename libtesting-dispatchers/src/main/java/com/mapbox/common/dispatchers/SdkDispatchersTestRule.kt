@file:OptIn(MapboxExperimental::class)

package com.mapbox.common.dispatchers

import com.mapbox.annotation.MapboxExperimental
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Overrides [SdkDispatchers.Default] and [SdkDispatchers.IO] with [dispatcher], and
 * [SdkDispatchers.Main] with [mainDispatcher], for the duration of the test, then restores
 * the SDK's real dispatchers.
 *
 * By default [mainDispatcher] is the same instance as [dispatcher]. Pass a different value
 * when a test needs to distinguish the main thread from the default/IO thread, for example
 * to assert that a callback is delivered on a specific dispatcher.
 *
 * Covers all three SDK dispatchers in a single rule, so there is no need to combine it with
 * `MainCoroutineRule` or call `Dispatchers.setMain` separately.
 */
@ExperimentalCoroutinesApi
class SdkDispatchersTestRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
    val mainDispatcher: TestDispatcher = dispatcher,
) : TestRule {

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        override fun evaluate() {
            SdkDispatchers.setTestMain(mainDispatcher)
            SdkDispatchers.setTestDispatchers(dispatcher)
            try {
                base.evaluate()
            } finally {
                SdkDispatchers.resetTestDispatchers()
                SdkDispatchers.resetTestMain()
            }
        }
    }
}
