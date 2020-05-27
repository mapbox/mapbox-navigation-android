package com.mapbox.navigation.utils.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * Will apply a function to each element in a list asynchronously.
 *
 * @param f a function that transforms the element in the list to another type
 * @param scope the coroutine scope to use for execute the asynchronous operation
 * @return a [List] of `B`
 */
fun <A, B> List<A>.parallelMap(f: (A) -> B, scope: CoroutineScope): List<B> = runBlocking {
    map { scope.async(Dispatchers.Default) { f(it) } }.map { it.await() }
}
