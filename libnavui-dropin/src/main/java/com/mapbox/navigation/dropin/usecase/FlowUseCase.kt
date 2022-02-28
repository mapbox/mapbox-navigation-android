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
 *   https://github.com/google/iosched/blob/main/shared/src/main/java/com/google/samples/apps/iosched/shared/domain/FlowUseCase.kt
 * CHANGES:
 *  - updated package
 *  - replaced `com.google.samples.apps.iosched.shared.result.Result` with `kotlin.Result`
 *  - updated comments
 */

package com.mapbox.navigation.dropin.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

/**
 * Executes business logic in its execute method and keep posting updates to the result as
 * [Result<R>].
 * Handling an exception (emit [Result.failure] to the result) is the subclasses responsibility.
 */
internal abstract class FlowUseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {
    operator fun invoke(parameters: P): Flow<Result<R>> = execute(parameters)
        .catch { e -> emit(Result.failure(Exception(e))) }
        .flowOn(coroutineDispatcher)

    protected abstract fun execute(parameters: P): Flow<Result<R>>
}
