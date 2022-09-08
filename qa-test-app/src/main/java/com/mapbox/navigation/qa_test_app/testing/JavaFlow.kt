package com.mapbox.navigation.qa_test_app.testing

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * This is not production tested. It is used to showcase java interoperability.
 */
object JavaFlow {

    @JvmStatic
    fun lifecycleScope(owner: LifecycleOwner): CoroutineScope = owner.lifecycleScope

    @JvmStatic
    fun <T> collect(flow: Flow<T>, scope: CoroutineScope, consumer: Consumer<T>) {
        scope.launch {
            flow.collect { value -> consumer.accept(value) }
        }
    }
}

interface Consumer<T> {
    fun accept(value: T)
}
