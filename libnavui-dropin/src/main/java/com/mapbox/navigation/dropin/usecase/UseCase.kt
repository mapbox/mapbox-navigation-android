/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * CLONED FROM:
 *   https://github.com/google/iosched/blob/main/shared/src/main/java/com/google/samples/apps/iosched/shared/domain/CoroutineUseCase.kt
 * CHANGES:
 *  - updated package
 *  - updated comments
 *  - updated filename
 *  - replaced `com.google.samples.apps.iosched.shared.result.Result` with `kotlin.Result`
 *  - replaced `timber.log.Timber` with `com.mapbox.navigation.utils.internal.logD`
 */

package com.mapbox.navigation.dropin.usecase

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Executes business logic synchronously or asynchronously using Coroutines.
 */
internal abstract class UseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {

    /** Executes the use case asynchronously and returns a [Result].
     *
     * @return a [Result].
     *
     * @param parameters the input parameters to run the use case with
     */
    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            // Moving all use case's executions to the injected dispatcher
            // In production code, this is usually the Default dispatcher (background thread)
            // In tests, this becomes a TestCoroutineDispatcher
            withContext(coroutineDispatcher) {
                execute(parameters).let {
                    Result.success(it)
                }
            }
        } catch (e: Exception) {
            logD(Tag(this.javaClass.simpleName), Message("error"), e)
            Result.failure(e)
        }
    }

    /**
     * Override this to set the code to be executed.
     */
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}
