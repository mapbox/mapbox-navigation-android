package com.mapbox.navigation.ui.app.internal.extension

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.testing.TestMiddleware
import com.mapbox.navigation.ui.app.testing.TestStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreSagaKtTest {

    private lateinit var store: TestStore

    @Before
    fun setUp() {
        store = TestStore()
    }

    @Test
    fun `takeAction - should delay coroutine until action is dispatched`() = runBlockingTest {
        val middleware = TestMiddleware()
        store.registerMiddleware(middleware)

        launch {
            store.takeAction<Action1>() // wait for Action1
            yield() // yielding to allow outer coroutine to resume
            store.dispatch(Action2)
        }
        store.dispatch(Action1)
        store.takeAction<Action2>() // wait for Action2

        assertEquals(listOf(Action1, Action2), middleware.actions)
    }

    @Test
    fun `actionsFlowable - should return cold flowable with dispatched actions`() =
        runBlockingTest {
            val actions = mutableListOf<Action>()
            val job = launch {
                store.actionsFlowable().take(2).toList(actions)
            }
            store.dispatch(Action1)
            store.dispatch(Action2)
            job.join()

            assertEquals(listOf(Action1, Action2), actions)
        }

    private object Action1 : Action
    private object Action2 : Action
}
