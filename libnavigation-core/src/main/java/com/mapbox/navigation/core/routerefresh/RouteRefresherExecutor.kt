package com.mapbox.navigation.core.routerefresh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private data class QueuedRequest(
    val routes: List<NavigationRoute>,
    val startCallback: () -> Unit,
    val finishCallback: (Expected<String, RoutesRefresherResult>) -> Unit,
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
    ): Expected<String, RoutesRefresherResult> = suspendCancellableCoroutine { cont ->
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
        finishCallback: (Expected<String, RoutesRefresherResult>) -> Unit,
    ) {
        queuedRequest?.finishCallback?.invoke(
            ExpectedFactory.createError("Skipping request as a newer one is queued.")
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
                localCurrentRequest.finishCallback(ExpectedFactory.createValue(result))
            }
        }
    }
}
