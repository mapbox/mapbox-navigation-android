package com.mapbox.navigation.ui.app.internal.extension

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.testing.TestReducer
import com.mapbox.navigation.ui.app.testing.TestStore
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class StoreThunkKtTest {

    private lateinit var sut: TestStore

    @Before
    fun setUp() {
        sut = TestStore()
    }

    @Test
    fun `should allow dispatch of ThunkActions`() {
        val action1 = object : Action {}
        val action2 = object : Action {}
        val action3 = object : Action {}
        val reducer = TestReducer()
        sut.register(reducer)

        sut.dispatch(
            ThunkAction {
                it.dispatch(action1)
                it.dispatch(action2)
                it.dispatch(action3)
            }
        )

        assertTrue(action1 in reducer.actions)
        assertTrue(action2 in reducer.actions)
        assertTrue(action3 in reducer.actions)
    }
}
