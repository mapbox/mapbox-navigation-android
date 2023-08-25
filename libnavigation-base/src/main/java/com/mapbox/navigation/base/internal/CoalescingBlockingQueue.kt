package com.mapbox.navigation.base.internal

import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

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
    private val hashCode: Int,
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
            val uuid = UUID.randomUUID().toString()
            logI("Mutex before lock startExecuting ($uuid) [$hashCode]", "MapboxRouteLineApi")
            mutex.withLock {
                try {
                    logI("Mutex after lock startExecuting ($uuid) [$hashCode]", "MapboxRouteLineApi")
                    val currentItemCopy = currentItem
                    currentItem = null
                    currentItemCopy?.block?.invoke()
                } catch (ex: CancellationException) {
                    logI("startExecuting cancelled ($uuid) [$hashCode]", "MapboxRouteLineApi")
                    throw ex
                }
            }
            logI("Mutex before unlock startExecuting ($uuid) [$hashCode]", "MapboxRouteLineApi")
        }
    }
}
