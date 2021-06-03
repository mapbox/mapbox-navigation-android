package com.mapbox.navigation.ui.maneuver

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.model.RoadShieldResult
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class RoadShieldContentManager {
    internal companion object {
        private val TAG = Tag("MbxRoadShieldContentManager")
        internal const val CANCELED_MESSAGE = "canceled"
    }

    private val maneuversToShieldsMap = hashMapOf<String, ByteArray?>()
    private val maneuversToFailuresMap = hashMapOf<String, RoadShieldError>()
    private val requestedShields = mutableListOf<String>()
    private val urlsToShieldsMap = hashMapOf<String, ByteArray?>()

    private val mainJob = ThreadController.getMainScopeAndRootJob()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    suspend fun getShields(
        maneuvers: List<Maneuver>
    ): RoadShieldResult {
        val idToUrlMap = hashMapOf<String, String?>()
        maneuvers.forEach { maneuver ->
            maneuver.primary.let {
                idToUrlMap[it.id] = it.componentList.findShieldUrl()
            }
            maneuver.secondary?.let {
                idToUrlMap[it.id] = it.componentList.findShieldUrl()
            }
            maneuver.sub?.let {
                idToUrlMap[it.id] = it.componentList.findShieldUrl()
            }
        }

        clearFailuresFor(idToUrlMap.keys)

        mainJob.scope.launch {
            prepareShields(idToUrlMap)
        }

        return try {
            waitForShields(idToUrlMap)
        } catch (ex: CancellationException) {
            val availableResult = generateResult(idToUrlMap)
            val canceled = idToUrlMap
                .filter {
                    !availableResult.shields.keys.contains(it.key) &&
                        !availableResult.errors.keys.contains(it.key)
                }
                .mapValues { RoadShieldError(it.value as String, CANCELED_MESSAGE) }

            return RoadShieldResult(
                availableResult.shields,
                availableResult.errors + canceled
            )
        }
    }

    private fun clearFailuresFor(keys: Set<String>) {
        keys.forEach {
            maneuversToFailuresMap.remove(it)
        }
    }

    fun cancelAll() {
        requestedShields.clear()
        mainJob.job.children.forEach { it.cancel() }
    }

    private suspend fun prepareShields(idToUrlMap: Map<String, String?>) {
        idToUrlMap.forEach { entry ->
            val id = entry.key
            val url = entry.value
            if (!maneuversToShieldsMap.containsKey(id) && !requestedShields.contains(id)) {
                if (url != null) {
                    val availableShield = urlsToShieldsMap[url]
                    if (availableShield != null) {
                        maneuversToShieldsMap[id] = availableShield
                    } else {
                        requestedShields.add(id)
                        mainJob.scope.launch {
                            RoadShieldDownloader.downloadImage(url).fold(
                                { error ->
                                    LoggerProvider.logger.e(TAG, Message(error))
                                    maneuversToFailuresMap[id] = RoadShieldError(url, error)
                                },
                                { value ->
                                    maneuversToShieldsMap[id] = value
                                    urlsToShieldsMap[url] = value
                                }
                            )
                            requestedShields.remove(id)
                            invalidate()
                        }
                    }
                } else {
                    maneuversToShieldsMap[id] = null
                }
            }
        }

        invalidate()
    }

    private suspend fun waitForShields(
        idToUrlMap: Map<String, String?>
    ): RoadShieldResult {
        return suspendCancellableCoroutine { continuation ->
            val callback = {
                check(!continuation.isCancelled)
                if (
                    idToUrlMap.keys.all {
                        maneuversToShieldsMap.containsKey(it) ||
                            maneuversToFailuresMap.containsKey(it)
                    }
                ) {
                    continuation.resume(generateResult(idToUrlMap))
                    true
                } else {
                    false
                }
            }
            if (callback()) {
                return@suspendCancellableCoroutine
            }
            awaitingCallbacks.add(callback)
            continuation.invokeOnCancellation {
                awaitingCallbacks.remove(callback)
            }
        }
    }

    private fun invalidate() {
        val iterator = awaitingCallbacks.iterator()
        while (iterator.hasNext()) {
            val remove = iterator.next().invoke()
            if (remove) {
                iterator.remove()
            }
        }
    }

    private fun generateResult(
        idToUrlMap: Map<String, String?>
    ): RoadShieldResult {
        return RoadShieldResult(
            maneuversToShieldsMap
                .filterKeys { idToUrlMap.keys.contains(it) }
                .mapValues {
                    // TODO: cleanup force casts?
                    val value = it.value
                    if (value != null) {
                        RoadShield(idToUrlMap[it.key]!!, value)
                    } else {
                        null
                    }
                },
            maneuversToFailuresMap.filterKeys { idToUrlMap.keys.contains(it) }
        )
    }
}

internal fun List<Component>.findShieldUrl(): String? {
    val node = this.find { it.node is RoadShieldComponentNode }?.node
    return if (node is RoadShieldComponentNode) {
        node.shieldUrl
    } else {
        null
    }
}
