@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.parser.directions

import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.base.internal.route.RoutesResponse.Metadata
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.RouteParsingTracking
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsRoutesParser
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRouteParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRoutesParser
import com.mapbox.navigation.base.internal.utils.RouteParsingQueue
import com.mapbox.navigation.base.internal.utils.RouteResponseInfo
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.runCatchingSuspend
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private const val LOG_CATEGORY = "NN-MODELS-PARALLEL-PARSING"
private const val PERFORMANCE_SECTION_NAME =
    "NnAndModelsParallelNavigationRoutesParser#parseResponse"

/**
 * Responsible for:
 * - Orchestration of NN([SDKRouteParser]) and one of [DirectionsRoutesParser] implementation to
 *   produce data needed for [NavigationRoute] creation
 * - reporting telemetry related to parsing like parsing time
 * - scheduling parsing to [RouteParsingQueue]
 */
internal class NnAndModelsParallelNavigationRoutesParser(
    private val routeParsingTracking: RouteParsingTracking,
    private val parsingDispatcher: CoroutineDispatcher =
        ThreadController.DefaultDispatcher,
    private val time: Time = Time.SystemClockImpl,
    private val modelParser: DirectionsRoutesParser,
    private val nnParser: SDKRouteParser,
    private val parsingQueue: RouteParsingQueue,
    private val logger: LoggerFrontend,
) : NavigationRoutesParser {

    override suspend fun parseDirectionsResponse(
        response: ResponseToParse,
    ): Result<NavigationRouteParsingSuccessfulResult> {
        val responseTimeElapsedMillis = time.millis()
        return parsingQueue.parseRouteResponse(
            RouteResponseInfo.fromResponse(response.responseBody.buffer),
        ) {
            withContext(parsingDispatcher) {
                parseRoutesInParallel(
                    response = response,
                    responseTimeElapsedMillis = responseTimeElapsedMillis,
                    parsingDispatcher = parsingDispatcher,
                    time = time,
                    modelParser = modelParser,
                    logger = logger,
                    routeParsingTracking = routeParsingTracking,
                    logCategory = LOG_CATEGORY,
                    performanceSectionName = PERFORMANCE_SECTION_NAME,
                    nativePerformanceSectionName = "SDKRouteParser#parseDirectionsResponse()",
                ) { responseBody, request, routerOrigin ->
                    nnParser.parseDirectionsResponse(responseBody, request, routerOrigin)
                }
            }
        }
    }
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal suspend fun parseRoutesInParallel(
    response: ResponseToParse,
    responseTimeElapsedMillis: Long,
    parsingDispatcher: CoroutineDispatcher,
    time: Time,
    modelParser: DirectionsRoutesParser,
    logger: LoggerFrontend,
    routeParsingTracking: RouteParsingTracking,
    logCategory: String,
    performanceSectionName: String,
    nativePerformanceSectionName: String,
    nativeParseFn: (DataRef, String, String) -> Expected<String, List<RouteInterface>>,
): Result<NavigationRouteParsingSuccessfulResult> = runCatchingSuspend {
    logger.logD(logCategory) {
        "parsing directions response"
    }
    val parsedResponse = PerformanceTracker.trackPerformanceAsync<RoutesResponse>(
        performanceSectionName,
    ) {
        logger.logI("parallel parsing started", logCategory)
        return@trackPerformanceAsync coroutineScope {
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

                PerformanceTracker.trackPerformanceSync("RouteModelsParser#parse") {
                    modelParser.parse(
                        response,
                    ).let {
                        val parseMillis = currentElapsedMillis() - startElapsedMillis
                        val parseThread = Thread.currentThread().name
                        logger.logD(logCategory) {
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

                val nativeParse = PerformanceTracker.trackPerformanceSync(
                    nativePerformanceSectionName,
                ) {
                    nativeParseFn(
                        response.responseBody,
                        response.routeRequest,
                        response.routerOrigin,
                    )
                }
                nativeParse.let {
                    val parseMillis = currentElapsedMillis() - startElapsedMillis
                    logger.logD(logCategory) {
                        "parsed directions response to RouteInterface " +
                            "for ${it.value?.firstOrNull()?.responseUuid}, " +
                            "parse time ${parseMillis}ms"
                    }
                    ParseResult(it, waitMillis, parseMillis)
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

                logger.logD(logCategory) {
                    "$performanceSectionName " +
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

    if (parsedResponse.routes.isEmpty()) {
        throw IllegalStateException("no routes returned, collection is empty")
    }
    routeParsingTracking.routeResponseIsParsed(
        parsedResponse.meta,
    )
    NavigationRouteParsingSuccessfulResult(
        parsedResponse.routes,
    )
}.onFailure {
    logger.logE("Route parsing failed: ${it.message}", logCategory)
}
