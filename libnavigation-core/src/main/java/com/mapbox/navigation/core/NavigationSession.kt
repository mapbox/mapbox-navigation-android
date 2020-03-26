package com.mapbox.navigation.core

import android.content.Context
import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.core.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.TAG
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.utils.thread.ThreadController
import java.lang.Exception
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

private data class SessionStateDescriptor(var job: Job?, val predicate: suspend (NavigationSession.State) -> Boolean)
private typealias SessionStatePredicate = suspend (NavigationSession.State) -> Boolean

internal class NavigationSession(private val context: Context) : RoutesObserver,
    TripSessionStateObserver {
    private val jobControl = ThreadController.getIOScopeAndRootJob()
    private val TAG = "MAPBOX_TELEMETRY"
    private class ReentrantCollection(val scope: CoroutineScope) {
        private val synchronizedCollection: MutableList<SessionStateDescriptor> = Collections.synchronizedList(mutableListOf<SessionStateDescriptor>())

        fun addItem(item: SessionStatePredicate) {
            synchronized(synchronizedCollection) {
                synchronizedCollection.add(0, SessionStateDescriptor(null, item))
            }
        }

        fun removeItem() {
            scope.launch {
                if (synchronizedCollection.isNotEmpty()) {
                    val index = synchronizedCollection.size - 1
                    synchronizedCollection.removeAt(index)
                }
            }
        }

        fun getCopy() =
            scope.async {
                val result = mutableListOf<SessionStatePredicate>()
                synchronizedCollection.forEach {
                    result.add(it.predicate)
                }
                result
            }

        fun clear() {
                scope.launch {
                val iterator = synchronizedCollection.iterator()
                while (iterator.hasNext()) {
                    val nextItem = iterator.next()
                    withContext(coroutineContext) {
                        nextItem.job?.cancelAndJoin()
                        Log.d(TAG, "Canceling and removing a single callback")
                    }
                }
                synchronizedCollection.clear()
            }
        }

        fun removeCanceledJobs() {
            val iterator = synchronizedCollection.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                ifNonNull(item.job) { job ->
                    Log.d(TAG, "removing canceled job ${job[CoroutineName.Key]}")
                    iterator.remove()
                }
            }
        }
        fun forEach(state: State) {
            scope.launch {
                removeCanceledJobs()
                val iterator = synchronizedCollection.iterator()
                while (iterator.hasNext()) {
                    val nextItem = iterator.next()
                    nextItem.job = scope.launch(CoroutineName(System.nanoTime().toString())) {
                        try {
                            if (!nextItem.predicate(state)) {
                                iterator.remove()
                                nextItem.job?.cancelAndJoin()
                                Log.d(TAG, "waiting for the job to finish before removing")
                            }
                        } catch (exception: Exception) {
                            Log.d(TAG, exception.localizedMessage)
                        }
                    }
                }
            }
        }

        fun size() = synchronizedCollection.size
    }

    private val stateObservers = CopyOnWriteArrayList<NavigationSessionStateObserver>()
    private val callbackList = ReentrantCollection(ThreadController.getIOScopeAndRootJob().scope)
    private var state = State.IDLE
        set(value) {
            if (field == value) {
                return
            }
            val previousValue = state
            field = value

            stateObservers.forEach { it.onNavigationSessionStateChanged(value) }
            callbackList.forEach(state)
            when {
                previousValue == State.ACTIVE_GUIDANCE -> MapboxNavigationAccounts.getInstance(
                    context.applicationContext
                ).navigationStopped()
                value == State.ACTIVE_GUIDANCE -> MapboxNavigationAccounts.getInstance(
                    context.applicationContext
                ).navigationStarted()
            }
        }

    private var hasRoutes = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    private var isDriving = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    private fun updateState() {
        state = when {
            hasRoutes && isDriving -> State.ACTIVE_GUIDANCE
            isDriving -> State.FREE_DRIVE
            else -> State.IDLE
        }
    }
    init {
        ThreadController.getIOScopeAndRootJob().scope.launch {
            select {
                jobControl.job.onJoin {
                    Log.d(TAG, "Called from onJoin(). Clearing the callback list")
                    callbackList.clear()
                }
            }
        }
    }
    internal fun getNavigatoinSessionState(predicate: suspend (State) -> Boolean) {
        callbackList.addItem { predicate(state) }
    }
    internal fun registerNavigationSessionStateObserver(navigationSessionStateObserver: NavigationSessionStateObserver) {
        stateObservers.add(navigationSessionStateObserver)
        navigationSessionStateObserver.onNavigationSessionStateChanged(state)
    }

    internal fun unregisterNavigationSessionStateObserver(navigationSessionStateObserver: NavigationSessionStateObserver) {
        stateObservers.remove(navigationSessionStateObserver)
    }

    internal fun unregisterAllNavigationSessionStateObservers() {
        stateObservers.clear()
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        hasRoutes = routes.isNotEmpty()
    }

    override fun onSessionStateChanged(tripSessionState: TripSessionState) {
        isDriving = when (tripSessionState) {
            TripSessionState.STARTED -> true
            TripSessionState.STOPPED -> {
                callbackList.clear()
                false
            }
        }
    }

    internal enum class State {
        IDLE,
        FREE_DRIVE,
        ACTIVE_GUIDANCE
    }
}
