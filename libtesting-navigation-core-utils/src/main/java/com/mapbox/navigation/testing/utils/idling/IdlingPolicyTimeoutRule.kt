package com.mapbox.navigation.testing.utils.idling

import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingPolicy
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.TimeUnit

/**
 * This allows you to override the idling error policies for your test.
 *
 * @param idleTimeout The amount of time the policy allows a resource to be non-idle.
 * @param idleTimeoutUnit unit of time for idleTimeout
 */
class IdlingPolicyTimeoutRule(
    private val idleTimeout: Long,
    private val idleTimeoutUnit: TimeUnit
) : TestWatcher() {

    lateinit var defaultMasterPolicy: IdlingPolicy
    lateinit var defaultErrorPolicy: IdlingPolicy

    override fun starting(description: Description?) {
        defaultMasterPolicy = IdlingPolicies.getMasterIdlingPolicy()
        defaultErrorPolicy = IdlingPolicies.getDynamicIdlingResourceErrorPolicy()
        IdlingPolicies.setMasterPolicyTimeout(idleTimeout, idleTimeoutUnit)
        IdlingPolicies.setIdlingResourceTimeout(idleTimeout, idleTimeoutUnit)
    }

    override fun finished(description: Description?) {
        IdlingPolicies.setMasterPolicyTimeout(
            defaultMasterPolicy.idleTimeout,
            defaultMasterPolicy.idleTimeoutUnit
        )
        IdlingPolicies.setIdlingResourceTimeout(
            defaultErrorPolicy.idleTimeout,
            defaultErrorPolicy.idleTimeoutUnit
        )
    }
}
