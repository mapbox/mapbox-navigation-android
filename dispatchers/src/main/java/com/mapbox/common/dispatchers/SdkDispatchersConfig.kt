@file:Suppress("ForbiddenImport")

package com.mapbox.common.dispatchers

import com.mapbox.annotation.MapboxExperimental
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@MapboxExperimental
class SdkDispatchersConfig private constructor(
    val defaultDispatcher: CoroutineDispatcher,
    val ioDispatcher: CoroutineDispatcher,
) {

    companion object {
        fun default(): SdkDispatchersConfig = Builder().build()
    }

    class Builder internal constructor() {
        private var customDefault: CoroutineDispatcher? = null
        private var customIO: CoroutineDispatcher? = null

        private var defaultParallelism: Int? = null
        private var ioParallelism: Int? = null

        /**
         * Provides a custom dispatcher for [SdkDispatchersConfig.defaultDispatcher]. Used as-is,
         * unless [setDefaultParallelism] is also called, in which case the parallelism limit is
         * applied on top of this dispatcher.
         */
        fun setDefault(dispatcher: CoroutineDispatcher) = apply {
            this.customDefault = dispatcher
        }

        /**
         * Provides a custom dispatcher for [SdkDispatchersConfig.ioDispatcher]. Used as-is,
         * unless [setIoParallelism] is also called, in which case the parallelism limit is
         * applied on top of this dispatcher.
         */
        fun setIO(dispatcher: CoroutineDispatcher) = apply {
            this.customIO = dispatcher
        }

        /**
         * Limits the parallelism of [SdkDispatchersConfig.defaultDispatcher]. Applies to the
         * dispatcher provided via [setDefault], or to the SDK's built-in default dispatcher if
         * [setDefault] was not called.
         */
        fun setDefaultParallelism(limit: Int) = apply {
            this.defaultParallelism = limit
        }

        /**
         * Limits the parallelism of [SdkDispatchersConfig.ioDispatcher]. Applies to the
         * dispatcher provided via [setIO], or to the SDK's built-in IO dispatcher if [setIO] was
         * not called.
         */
        fun setIoParallelism(limit: Int) = apply {
            this.ioParallelism = limit
        }

        internal fun build(): SdkDispatchersConfig {
            val default = resolve(customDefault, defaultParallelism, Dispatchers.Default)
            val io = resolve(customIO, ioParallelism, Dispatchers.IO)

            return SdkDispatchersConfig(default, io)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun resolve(
            custom: CoroutineDispatcher?,
            parallelism: Int?,
            builtin: CoroutineDispatcher,
        ): CoroutineDispatcher = when {
            custom != null && parallelism != null -> custom.limitedParallelism(parallelism)
            custom != null -> custom
            parallelism != null -> builtin.limitedParallelism(parallelism)
            else -> builtin
        }
    }
}
