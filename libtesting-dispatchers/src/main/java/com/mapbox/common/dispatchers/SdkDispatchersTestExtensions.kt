@file:Suppress("ForbiddenImport")
@file:OptIn(MapboxExperimental::class)

package com.mapbox.common.dispatchers

import com.mapbox.annotation.MapboxExperimental

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Overrides [SdkDispatchers.Default] and [SdkDispatchers.IO] with [dispatcher] for the duration
 * of a test. Call [SdkDispatchers.resetTestDispatchers] in `@After` to restore the SDK's real
 * dispatchers, or use [SdkDispatchersTestRule] to handle all three dispatchers automatically.
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun SdkDispatchers.setTestDispatchers(dispatcher: CoroutineDispatcher) =
    setTestDispatcher(dispatcher)

/**
 * Restores [SdkDispatchers.Default] and [SdkDispatchers.IO] to the SDK's real dispatchers after
 * a test override installed by [SdkDispatchers.setTestDispatchers].
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun SdkDispatchers.resetTestDispatchers() = resetTestDispatcher()

/**
 * Overrides [SdkDispatchers.Main] (i.e. `Dispatchers.Main`) with [dispatcher] for the duration
 * of a test. Call [SdkDispatchers.resetTestMain] in `@After`, or use [SdkDispatchersTestRule]
 * to handle all three dispatchers automatically.
 */
@ExperimentalCoroutinesApi
fun SdkDispatchers.setTestMain(dispatcher: TestDispatcher) = Dispatchers.setMain(dispatcher)

/**
 * Restores [SdkDispatchers.Main] to the real main dispatcher after a test override installed by
 * [SdkDispatchers.setTestMain].
 */
@ExperimentalCoroutinesApi
fun SdkDispatchers.resetTestMain() = Dispatchers.resetMain()
