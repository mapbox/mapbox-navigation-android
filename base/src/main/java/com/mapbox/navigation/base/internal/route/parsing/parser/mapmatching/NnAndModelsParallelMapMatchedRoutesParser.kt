package com.mapbox.navigation.base.internal.route.parsing.parser.mapmatching

import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.RoutesResponse.Metadata
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.RouteParsingTracking
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedRoutesParser
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchingMatchParser
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchingMatchParsingSuccessfulResult
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

private const val LOG_CATEGORY = "NN-MODELS-PARALLEL-MAP-MATCHED-PARSING"

internal class NnAndModelsParallelMapMatchedRoutesParser(
    private val routeParsingTracking: RouteParsingTracking,
    private val parsingDispatcher: CoroutineDispatcher = ThreadController.DefaultDispatcher,
    private val time: Time = Time.SystemClockImpl,
    private val modelParser: MapMatchedRoutesParser,
    private val nnParser: SDKRouteParser,
    private val parsingQueue: RouteParsingQueue,
    private val logger: LoggerFrontend,
) : MapMatchingMatchParser {

    override suspend fun parseMapMatchedResponse(
        response: ResponseToParse,
    ): Result<MapMatchingMatchParsingSuccessfulResult> {
        val responseTimeElapsedMillis = time.millis()
        return parsingQueue.parseRouteResponse(
            RouteResponseInfo.fromResponse(response.responseBody.buffer),
        ) {
            withContext(parsingDispatcher) {
                parseMapMatchedRoutesInParallel(
                    response = response,
                    responseTimeElapsedMillis = responseTimeElapsedMillis,
                    parsingDispatcher = parsingDispatcher,
                    time = time,
                    modelParser = modelParser,
                    logger = logger,
                    routeParsingTracking = routeParsingTracking,
                ) { responseBody, request, routerOrigin ->
                    nnParser.parseMapMatchedResponse(responseBody, request, routerOrigin)
                }
            }
        }
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal suspend fun parseMapMatchedRoutesInParallel(
    response: ResponseToParse,
    responseTimeElapsedMillis: Long,
    parsingDispatcher: CoroutineDispatcher,
    time: Time,
    modelParser: MapMatchedRoutesParser,
    logger: LoggerFrontend,
    routeParsingTracking: RouteParsingTracking,
    nativeParseFn: (DataRef, String, String) -> Expected<String, List<RouteInterface>>,
): Result<MapMatchingMatchParsingSuccessfulResult> = runCatchingSuspend {
    logger.logD(LOG_CATEGORY) { "parsing map matched response" }

    data class ParseResult<T>(
        val value: T,
        val waitMillis: Long,
        val parseMillis: Long,
        val threadName: String = "",
    )

    fun currentElapsedMillis() = time.millis()

    val (matches, meta) = coroutineScope {
        val deferredModelParsing = async(parsingDispatcher) {
            val startElapsedMillis = currentElapsedMillis()
            val waitMillis = startElapsedMillis - responseTimeElapsedMillis
            PerformanceTracker.trackPerformanceSync("MapMatchingModelsParser#parse") {
                modelParser.parse(response).let {
                    val parseMillis = currentElapsedMillis() - startElapsedMillis
                    logger.logD(LOG_CATEGORY) {
                        it.map {
                            "parsed map matching response to public API models, " +
                                "parse time ${parseMillis}ms"
                        }.getOrElse { "failed to parse response, time ${parseMillis}ms" }
                    }
                    ParseResult(it, waitMillis, parseMillis, Thread.currentThread().name)
                }
            }
        }
        val deferredNativeParsing = async(parsingDispatcher) {
            val startElapsedMillis = currentElapsedMillis()
            val waitMillis = startElapsedMillis - responseTimeElapsedMillis
            val nativeParse = PerformanceTracker.trackPerformanceSync(
                "SDKRouteParser#parseMapMatchedResponse()",
            ) {
                nativeParseFn(response.responseBody, response.routeRequest, response.routerOrigin)
            }
            nativeParse.let {
                val parseMillis = currentElapsedMillis() - startElapsedMillis
                logger.logD(LOG_CATEGORY) {
                    "parsed map matched response to RouteInterface, parse time ${parseMillis}ms"
                }
                ParseResult(it, waitMillis, parseMillis)
            }
        }

        val nativeParseResult = deferredNativeParsing.await()
        val modelParseResult = deferredModelParsing.await()

        val matches = NavigationRoute.createFromMapMatchingResult(
            nativeParseResult.value,
            modelParseResult.value.getOrThrow(),
            responseTimeElapsedMillis.milliseconds.inWholeSeconds,
        )

        val totalParseMillis = modelParseResult.parseMillis + nativeParseResult.parseMillis
        logger.logD(LOG_CATEGORY) {
            val id = matches.firstOrNull()?.navigationRoute?.id
            "total parse time ${totalParseMillis}ms for $id"
        }

        val meta = Metadata(
            createdAtElapsedMillis = currentElapsedMillis(),
            responseWaitMillis = modelParseResult.waitMillis,
            responseParseMillis = modelParseResult.parseMillis,
            responseParseThread = modelParseResult.threadName,
            nativeWaitMillis = nativeParseResult.waitMillis,
            nativeParseMillis = nativeParseResult.parseMillis,
        )
        Pair(matches, meta)
    }

    if (matches.isEmpty()) {
        throw IllegalStateException("no routes returned, collection is empty")
    }
    routeParsingTracking.routeResponseIsParsed(meta)
    MapMatchingMatchParsingSuccessfulResult(matches)
}.onFailure {
    logger.logE("Map matched route parsing failed: ${it.message}", LOG_CATEGORY)
}
