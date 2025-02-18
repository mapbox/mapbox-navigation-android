package com.mapbox.navigation.mapgpt.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

fun interface MiddlewareContextFactory<Input : MiddlewareContext, Output : MiddlewareContext> {
    fun Input.build(): Output
}

/**
 * This registry allows you to keep track of multiple middleware and multiple contexts. It defines
 * a single level hierarchy where there is the parent context, and optional children contexts.
 *
 * You can define a parent [MiddlewareContext] for the registry. Children middleware will have
 * access to the parent context as long as they are registered and the parent context is attached.
 * You can also define [MiddlewareContextFactory] which is used to construct context from the
 * parent [MiddlewareContext]. The child context can not have a lifecycle outside of the parent
 * context; in other words, when the parent context is detached, all the children are detached.
 * You control the lifecycle of the children context with the [register], [unregister] functions.
 */
open class MiddlewareRegistry<Context : MiddlewareContext> : CoroutineMiddleware<Context>() {
    private val _middlewares = MutableStateFlow<Set<Middleware<*>>>(emptySet())
    private val _factories = mutableMapOf<Middleware<*>, MiddlewareContextFactory<*, *>>()
    private val _contexts = mutableMapOf<Middleware<*>, Any>()

    val middlewares: StateFlow<Set<Middleware<*>>> = _middlewares.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    override fun onAttached(middlewareContext: Context) {
        super.onAttached(middlewareContext)
        _middlewares.value.forEach { middleware ->
            val context: Context = _factories[middleware]
                ?.let { it as MiddlewareContextFactory<Context, *> }
                ?.let { factory ->
                    with (factory) { middlewareContext.build() } as Context
                } ?: middlewareContext
            _contexts[middleware] = context
            (middleware as? Middleware<Context>)?.onAttached(context)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onDetached(middlewareContext: Context) {
        super.onDetached(middlewareContext)
        _middlewares.value.reversed().forEach { middleware ->
            val context: Context = _contexts.remove(middleware)
                ?.let { (it as Context) }
                ?: middlewareContext
            (middleware as? Middleware<Context>)?.onDetached(context)
        }
    }

    fun context(): Flow<Context?> = middlewareContextFlow

    suspend fun repeatOnAttached(block: suspend Context.() -> Unit) {
        var currentContextJob: Job? = null
        context().collectLatest { currentContext ->
            currentContextJob?.cancel()
            currentContextJob = mainScope.launch {
                currentContext?.let { it.block() }
            }
        }
    }

    fun launchOnAttached(block: suspend Context.() -> Unit): Job {
        return stateScope.launch {
            repeatOnAttached(block)
        }
    }

    fun <R> execute(block: Context.() -> R): R? {
        return middlewareContext?.block()
    }

    inline fun <reified C: MiddlewareContext, reified M : Middleware<C>> middlewares(): List<M> {
        return middlewares.value.filterIsInstance<M>()
    }

    fun register(middleware: Middleware<Context>) = apply {
        _middlewares.update { before ->
            return@update if (before.contains(middleware)) {
                before
            } else {
                val after = before.toMutableSet()
                after.add(middleware)
                middlewareContext?.let { middleware.onAttached(it) }
                after
            }
        }
    }

    fun <C : MiddlewareContext> register(
        middleware: Middleware<C>,
        factory: MiddlewareContextFactory<Context, C>,
    ) = apply {
        _middlewares.update { before ->
            return@update if (before.contains(middleware)) {
                before
            } else {
                val after = before.toMutableSet()
                _factories[middleware] = factory
                after.add(middleware)
                middlewareContext?.let {
                    val context = with(factory) { it.build() }
                    _contexts[middleware] = context
                    middleware.onAttached(context)
                }
                after
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <C : Context> unregister(middleware: Middleware<C>) = apply {
        _middlewares.update { before ->
            return@update if (before.contains(middleware)) {
                val after = before.toMutableSet()
                after.remove(middleware)
                val context = (_contexts.remove(middleware) ?: middlewareContext) as? C
                context?.let { middleware.onDetached(it) }
                after
            } else {
                before
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun unregisterAll() = apply {
        _middlewares.value.forEach { middleware ->
            val typedMiddleware = middleware as Middleware<MiddlewareContext>
            _contexts.remove(middleware)?.let { context ->
                val typedContext = context as MiddlewareContext
                typedMiddleware.onDetached(typedContext)
            }
        }
        _middlewares.value = emptySet()
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Middleware<Context>> getMiddlewares(clazz: KClass<out M>): List<M> {
        return middlewares.value.filter { clazz.isInstance(it) } as List<M>
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Middleware<Context>> getMiddleware(clazz: KClass<out M>): M? {
        return middlewares.value.firstOrNull { clazz.isInstance(it) } as? M
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Middleware<Context>> observeMiddlewares(clazz: KClass<out M>): Flow<Set<M>> {
        return middlewares.map { middlewares ->
            middlewares.filter { clazz.isInstance(it) }.toSet() as Set<M>
        }
    }

    protected fun requireContext(): Context = middlewareContext ?: run {
        throw Error("$TAG must be attached to use context")
    }

    private companion object {
        private const val TAG = "MiddlewareRegistry"
    }
}
