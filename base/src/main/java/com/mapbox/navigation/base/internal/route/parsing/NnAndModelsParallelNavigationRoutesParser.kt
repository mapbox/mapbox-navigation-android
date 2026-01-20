@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.base.internal.route.RoutesResponse.Metadata
import com.mapbox.navigation.base.internal.route.parsing.models.RouteModelsParser
import com.mapbox.navigation.base.internal.utils.RouteParsingQueue
import com.mapbox.navigation.base.internal.utils.RouteResponseInfo
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.runCatchingSuspend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private const val LOG_CATEGORY = "NN-MODELS-PARALLEL-PARSING"

/**
 * Responsible for:
 * - Orchestration of NN([SDKRouteParser]) and one of [RouteModelsParser] implementation to
 *   produce data needed for [NavigationRoute] creation
 * - reporting telemetry related to parsing like parsing time
 * - scheduling parsing to [RouteParsingQueue]
 */
internal class NnAndModelsParallelNavigationRoutesParser(
    private val routeParsingTracking: RouteParsingTracking,
    private val parsingDispatcher: CoroutineDispatcher =
        ThreadController.Companion.DefaultDispatcher,
    private val time: Time = Time.SystemClockImpl,
    private val modelParser: RouteModelsParser,
    private val nnParser: SDKRouteParser,
    private val parsingQueue: RouteParsingQueue,
    private val logger: LoggerFrontend,
) : NavigationRoutesParser {

    override suspend fun parseDirectionsResponse(
        response: DirectionsResponseToParse,
    ): Result<DirectionsResponseParsingSuccessfulResult> {
        val responseTimeElapsedMillis = time.millis()
        return parsingQueue.parseRouteResponse(
            RouteResponseInfo.fromResponse(response.responseBody.buffer),
        ) {
            withContext(parsingDispatcher) {
                parseResponse(response, responseTimeElapsedMillis)
            }
        }
    }

    private suspend fun parseResponse(
        response: DirectionsResponseToParse,
        responseTimeElapsedMillis: Long,
    ): Result<DirectionsResponseParsingSuccessfulResult> = runCatchingSuspend {
        logger.logD(LOG_CATEGORY) {
            "parsing directions response"
        }
        val response = PerformanceTracker.trackPerformanceAsync<RoutesResponse>(
            "NnAndModelsParallelNavigationRoutesParser#parseResponse",
        ) {
            logger.logI(
                "parallel parsing started",
                LOG_CATEGORY,
            )

            return@trackPerformanceAsync coroutineScope<RoutesResponse> {
                data class ParseResult<T>(
                    val value: T,
                    val waitMillis: Long,
                    val parseMillis: Long,
                    val threadName: String = "",
                )

                fun currentElapsedMillis() = time.millis()

                val deferredResponseParsing = async(parsingDispatcher) {
                    val startElapsedMillis = currentElapsedMillis()
                    val waitMillis = startElapsedMillis - responseTimeElapsedMillis

                    PerformanceTracker.trackPerformanceSync(
                        "RouteModelsParser#parse",
                    ) {
                        modelParser.parse(
                            response,
                        ).let {
                            val parseMillis = currentElapsedMillis() - startElapsedMillis
                            val parseThread = Thread.currentThread().name
                            logger.logD(LOG_CATEGORY) {
                                it.map {
                                    "parsed directions response to public API models " +
                                        "for ${it.responseUUID}, " +
                                        "parse time ${parseMillis}ms"
                                }.getOrElse { "failed to parse response, time ${parseMillis}ms" }
                            }
                            ParseResult(it, waitMillis, parseMillis, parseThread)
                        }
                    }
                }
                val deferredNativeParsing = async(parsingDispatcher) {
                    val startElapsedMillis = currentElapsedMillis()
                    val waitMillis = startElapsedMillis - responseTimeElapsedMillis

                    PerformanceTracker.trackPerformanceSync(
                        "SDKRouteParser#parseDirectionsResponse()",
                    ) {
                        nnParser.parseDirectionsResponse(
                            response = response.responseBody,
                            request = response.routeRequest,
                            routerOrigin = response.routerOrigin,
                        ).let {
                            val parseMillis = currentElapsedMillis() - startElapsedMillis
                            logger.logD(LOG_CATEGORY) {
                                "parsed directions response to RouteInterface " +
                                    "for ${it.value?.firstOrNull()?.responseUuid}, " +
                                    "parse time ${parseMillis}ms"
                            }
                            ParseResult(it, waitMillis, parseMillis)
                        }
                    }
                }

                val nativeParseResult = deferredNativeParsing.await()
                val responseParseResult = deferredResponseParsing.await()

                NavigationRoute.create(
                    nativeParseResult.value,
                    responseParseResult.value.getOrThrow(),
                    responseTimeElapsedMillis.milliseconds.inWholeSeconds,
                    response.responseOriginAPI,
                ).let { routes ->
                    val totalParseMillis = responseParseResult.parseMillis +
                        nativeParseResult.parseMillis

                    logger.logD(LOG_CATEGORY) {
                        "NnAndModelsParallelNavigationRoutesParser#parseResponse " +
                            "for ${routes.firstOrNull()?.responseUUID}," +
                            "total parse time ${totalParseMillis}ms"
                    }

                    RoutesResponse(
                        routes = routes,
                        meta = Metadata(
                            createdAtElapsedMillis = currentElapsedMillis(),
                            responseWaitMillis = responseParseResult.waitMillis,
                            responseParseMillis = responseParseResult.parseMillis,
                            responseParseThread = responseParseResult.threadName,
                            nativeWaitMillis = nativeParseResult.waitMillis,
                            nativeParseMillis = nativeParseResult.parseMillis,
                        ),
                    )
                }
            }
        }

        if (response.routes.isEmpty()) {
            throw IllegalStateException("no routes returned, collection is empty")
        }
        routeParsingTracking.routeResponseIsParsed(
            response.meta,
        )
        DirectionsResponseParsingSuccessfulResult(
            response.routes,
        )
    }.onFailure {
        logger.logE("Route parsing failed: ${it.message}", LOG_CATEGORY)
    }
}
