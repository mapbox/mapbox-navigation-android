package com.mapbox.navigation.testing.ui.assertions

import com.mapbox.navigation.testing.ui.assertions.ExpectedValue.Optional
import com.mapbox.navigation.testing.ui.assertions.ExpectedValue.Required
import org.junit.Assert

/**
 * A class that allows to collect samples of state changes of a type [T].
 *
 * The samples can later be verified whether they occurred in the desired order.
 * The class allows to recognize between samples that are [Required] and [Optional].
 *
 * The values at a certain step of assertion can be invoked any number of times.
 * Currently, there's no option to assert the number of occurrences of certain states, only their order.
 *
 * Example usage:
 * ```
 * // create an assertion object with expected states
 * val assertion = ValueTransitionAssertion() {
 *    optionalState(some_state)
 *    requiredState(other_state)
 *    requiredState(some_state)
 *  }
 *
 *  // start collecting values
 *  assertion.onNewValue(value)
 *  ...
 *
 *  // assert results
 *  assertion.assert()
 * ```
 * In this example, the assertion will verify that:
 * 1. `some_state` might've occurred.
 * 2. Then, `other_state` definitely occurred.
 * 3. And was definitely followed by `some_state`.
 */
abstract class ValueTransitionAssertion<T>(
    expectedBlock: ValueTransitionAssertion<T>.() -> Unit
) : NavigationAssertion {
    private val actualValues = mutableListOf<T>()
    private val expectedValues = mutableListOf<ExpectedValue<T>>()

    init {
        expectedBlock()
    }

    override fun assert() {
        var minIndex = 0
        actualValues.forEach { state ->
            val minRequiredIndex =
                expectedValues.indexOfFirst { it is Required && expectedValues.indexOf(it) > minIndex }
            val availableStates =
                expectedValues.filterIndexed { index, _ ->
                    if (minRequiredIndex >= 0) {
                        index in minIndex..minRequiredIndex
                    } else {
                        index >= minIndex
                    }
                }
            when (val matching = availableStates.find { it.value == state }) {
                is Required -> {
                    minIndex = expectedValues.indexOf(matching)
                }
                is Optional -> {
                    // do nothing
                }
                null -> Assert.fail(
                    """$state shouldn't be returned right now. Expected are:
                    |$availableStates
                    |Full list of transitions:
                    |${actualValues}
                """.trimMargin()
                )
            }
        }

        check(expectedValues.indexOfLast { it is Required }
            .let { if (it == -1) 0 else it } == minIndex) {
            """Not all of the expected states were matched. Below were not matched:
               |${expectedValues.drop(minIndex).filterIsInstance<Required<T>>()}
               |Full list of transitions that were captured:
               |${actualValues}
            """.trimMargin()
        }
    }

    /**
     * Adds a [Required] state to the assertion expectations.
     */
    fun requiredState(value: T) {
        expectedValues.add(Required(value))
    }

    /**
     * Adds an [Optional] state to the assertion expectations.
     */
    fun optionalState(value: T) {
        expectedValues.add(Optional(value))
    }

    /**
     * Invoke to record a new sample.
     */
    fun onNewValue(value: T) = actualValues.add(value)
}

private sealed class ExpectedValue<T>(val value: T) {
    class Required<T>(value: T) : ExpectedValue<T>(value)

    class Optional<T>(value: T) : ExpectedValue<T>(value)

    override fun toString(): String {
        return "${this::class.simpleName}(value=$value)"
    }
}
