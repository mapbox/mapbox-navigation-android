@file:Suppress("ForbiddenImport")
@file:OptIn(MapboxExperimental::class)

package com.mapbox.common.dispatchers

import com.mapbox.annotation.MapboxExperimental
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * End-to-end check that [SdkDispatchersTestRule] installs and restores all dispatchers
 * correctly around a test.
 *
 * Drives [SdkDispatchersTestRule.apply] manually, rather than declaring it with `@get:Rule`, so
 * both the "during the test" and "after the test" assertions can be made from the same method.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SdkDispatchersTestRuleTest {

    @Test
    fun rule_overridesDefaultAndIO_duringTheTest_thenRestoresAfterward() {
        val testDispatcher = UnconfinedTestDispatcher()
        val rule = SdkDispatchersTestRule(testDispatcher)
        var observedDefault: CoroutineDispatcher? = null

        val statement = rule.apply(
            object : Statement() {
                override fun evaluate() {
                    observedDefault = SdkDispatchers.Default
                    assertSame(Dispatchers.Main, SdkDispatchers.Main)
                }
            },
            Description.EMPTY,
        )
        statement.evaluate()

        assertSame(testDispatcher, observedDefault)
        assertSame(Dispatchers.Default, SdkDispatchers.Default)
    }
}
