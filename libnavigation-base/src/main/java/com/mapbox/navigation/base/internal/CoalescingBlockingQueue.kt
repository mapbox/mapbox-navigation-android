package com.mapbox.navigation.base.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A queue that executes added jobs (see [addJob]) in the order they are added,
 * within provided scope in a blocking way (under the lock of provided mutex).
 * If a new job is added while the previous one is not yet started (it's still waiting to acquire the lock),
 * the previous job ([Item.block]) will not be be executed
 * and the corresponding cancellation (see [Item.cancellation]) will be invoked.
 * This way in case there are multiple jobs in the queue, only the latest one will be executed.
 * NOTE: the queue is not thread safe. Add jobs from a single thread or add synchronization
 * on your side to avoid concurrency problems.
 */
class CoalescingBlockingQueue(
    private val scope: CoroutineScope,
    private val mutex: Mutex,
) {

    data class Item(
        val block: () -> Unit,
        val cancellation: () -> Unit,
    )

    private var currentItem: Item? = null

    fun addJob(item: Item) {
        currentItem?.cancellation?.invoke()
        currentItem = item
        startExecuting()
    }

    private fun startExecuting() {
        scope.launch {
            mutex.withLock {
                val currentItemCopy = currentItem
                currentItem = null
                currentItemCopy?.block?.invoke()
            }
        }
    }
}
