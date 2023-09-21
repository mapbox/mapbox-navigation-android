package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal sealed class RoutesRefresherExecutorResult {

    internal data class Finished(val value: RoutesRefresherResult) : RoutesRefresherExecutorResult()

    internal object ReplacedByNewer : RoutesRefresherExecutorResult()
}

private data class QueuedRequest(
    val routes: List<NavigationRoute>,
    val startCallback: () -> Unit,
    val finishCallback: (RoutesRefresherExecutorResult) -> Unit,
)

internal class RouteRefresherExecutor(
    private val routeRefresher: RouteRefresher,
    private val timeout: Long,
    private val scope: CoroutineScope,
) {

    private var currentRequest: QueuedRequest? = null
    private var queuedRequest: QueuedRequest? = null

    suspend fun executeRoutesRefresh(
        routes: List<NavigationRoute>,
        startCallback: () -> Unit,
    ): RoutesRefresherExecutorResult = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation {
            currentRequest = null
            queuedRequest = null
        }
        executeRoutesRefresh(routes, startCallback) {
            cont.resume(it)
        }
    }

    private fun executeRoutesRefresh(
        routes: List<NavigationRoute>,
        startCallback: () -> Unit,
        finishCallback: (RoutesRefresherExecutorResult) -> Unit,
    ) {
        queuedRequest?.finishCallback?.invoke(
            RoutesRefresherExecutorResult.ReplacedByNewer
        )
        queuedRequest = QueuedRequest(routes, startCallback, finishCallback)
        runQueue()
    }

    private fun runQueue() {
        if (currentRequest == null && queuedRequest != null) {
            val localCurrentRequest = queuedRequest!!.also { currentRequest = it }
            queuedRequest = null
            localCurrentRequest.startCallback()
            scope.launch {
                val result = try {
                    routeRefresher.refresh(localCurrentRequest.routes, timeout)
                } finally {
                    currentRequest = null
                }
                runQueue()
                localCurrentRequest.finishCallback(RoutesRefresherExecutorResult.Finished(result))
            }
        }
    }
}
