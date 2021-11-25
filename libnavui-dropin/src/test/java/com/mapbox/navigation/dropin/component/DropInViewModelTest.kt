package com.mapbox.navigation.dropin.component

import com.mapbox.navigation.dropin.component.DropInViewModelTest.ProcessorMock
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class DropInViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `consumer is notified with default state`() = coroutineRule.runBlockingTest {
        val initialValue = mockk<DummyState>()
        val viewModel = DummyViewModel(initialValue)

        val collector: suspend (DummyState) -> Unit = mockk(relaxed = true)
        viewModel.state.collectAndCancel(this, collector)

        coVerify {
            collector(initialValue)
        }
    }

    @Test
    fun `consumer is notified with processed value`() = coroutineRule.runBlockingTest {
        val initialValue = mockk<DummyState>()
        val action = mockk<DummyAction>()
        val result = mockk<DummyState>()
        val processor: ProcessorMock = mockk {
            coEvery { process(initialValue, action) } returns result
        }
        val viewModel = DummyViewModel(initialValue, processor)

        viewModel.consumeAction(flowOf(action))
        val collector: suspend (DummyState) -> Unit = mockk(relaxed = true)
        viewModel.state.collectAndCancel(this, collector)

        coVerify {
            collector(result)
        }
    }

    @Test
    fun `actions are processed in order`() = coroutineRule.runBlockingTest {
        val initialValue = mockk<DummyState>()
        val action1 = mockk<DummyAction>()
        val result1 = mockk<DummyState>()
        val action2 = mockk<DummyAction>()
        val result2 = mockk<DummyState>()
        val processor: ProcessorMock = mockk {
            coEvery { process(initialValue, action1) } coAnswers {
                delay(500)
                result1
            }
            coEvery { process(result1, action2) } coAnswers {
                result2
            }
            coEvery { process(initialValue, action2) } coAnswers {
                throw IllegalArgumentException(
                    "second action should be invoked with the result of first action"
                )
            }
        }
        val viewModel = DummyViewModel(initialValue, processor)

        pauseDispatcher {
            viewModel.consumeAction(flowOf(action1))
            viewModel.consumeAction(flowOf(action2))
            runCurrent()
        }
        val collector: suspend (DummyState) -> Unit = mockk(relaxed = true)
        viewModel.state.collectAndCancel(this, collector)

        coVerify {
            processor.process(initialValue, action1)
        }
        coVerify {
            processor.process(result1, action2)
        }
        coVerify {
            collector(result2)
        }
    }

    class DummyState
    private class DummyAction
    private class DummyViewModel(
        initialValue: DummyState = DummyState(),
        private val processor: ProcessorMock = ProcessorMock { _, _ -> DummyState() }
    ) : DropInViewModel<DummyState, DummyAction>(initialValue) {
        override suspend fun process(
            accumulator: DummyState,
            value: DummyAction
        ) = processor.process(accumulator, value)
    }

    private fun interface ProcessorMock {
        suspend fun process(accumulator: DummyState, value: DummyAction): DummyState
    }

    private fun Flow<DummyState>.collectAndCancel(
        scope: CoroutineScope,
        collector: suspend (DummyState) -> Unit
    ) {
        val job = scope.launch {
            collect(collector)
        }
        job.cancel()
    }
}
