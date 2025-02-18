package com.mapbox.navigation.core.internal

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import com.mapbox.common.MemoryMonitorFactory
import com.mapbox.common.MemoryMonitorInterface
import com.mapbox.common.MemoryMonitorObserver
import com.mapbox.common.MemoryMonitorState
import com.mapbox.navigation.utils.internal.logD
import java.util.concurrent.CopyOnWriteArrayList

@RestrictTo(LIBRARY_GROUP_PREFIX)
interface LowMemoryManager {

    fun addObserver(observer: Observer)

    fun removeObserver(observer: Observer)

    fun interface Observer {
        fun onLowMemory()
    }

    companion object {
        fun create(): LowMemoryManager = LowMemoryManagerImpl()
    }
}

internal class LowMemoryManagerImpl(
    private val memoryMonitor: MemoryMonitorInterface = MemoryMonitorFactory.getOrCreate(),
) : LowMemoryManager {

    private val observers = CopyOnWriteArrayList<LowMemoryManager.Observer>()

    private val memoryMonitorObserver = MemoryMonitorObserver { status ->
        if (status.state == MemoryMonitorState.MEMORY_THRESHOLD_REACHED ||
            status.state == MemoryMonitorState.SYSTEM_MEMORY_WARNING_RECEIVED
        ) {
            logD(LOG_CATEGORY) { "onMemoryMonitorAlert($status). Notifying about low memory..." }
            observers.forEach {
                it.onLowMemory()
            }
        }
    }

    override fun addObserver(observer: LowMemoryManager.Observer) {
        synchronized(observers) {
            observers.add(observer)
            if (observers.size == 1) {
                memoryMonitor.registerObserver(memoryMonitorObserver)
            }
        }
    }

    override fun removeObserver(observer: LowMemoryManager.Observer) {
        synchronized(observers) {
            observers.remove(observer)
            if (observers.isEmpty()) {
                memoryMonitor.unregisterObserver(memoryMonitorObserver)
            }
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "LowMemoryManager"
    }
}
