package com.mapbox.navigation.base.middleware

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
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

/**
 * Factory uses to convert [Input] middleware context to [Output] context.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface MiddlewareContextFactory<Input : MiddlewareContext, Output : MiddlewareContext> {

    /**
     * Convert [Input] middleware context to [Output] context.
     */
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
@ExperimentalPreviewMapboxNavigationAPI
open class MiddlewareRegistry<Context : MiddlewareContext> : CoroutineMiddleware<Context>() {

    private val _middlewares = MutableStateFlow<Set<Middleware<*>>>(emptySet())
    private val _factories = mutableMapOf<Middleware<*>, MiddlewareContextFactory<*, *>>()
    private val _contexts = mutableMapOf<Middleware<*>, Any>()

    /**
     * A flow representing the current set of registered middleware instances.
     */
    val middlewares: StateFlow<Set<Middleware<*>>> = _middlewares.asStateFlow()

    /**
     * Attaches all registered middleware instances when the registry is attached
     *
     * @param middlewareContext The context providing information about the middleware state
     */
    @Suppress("UNCHECKED_CAST")
    override fun onAttached(middlewareContext: Context) {
        super.onAttached(middlewareContext)
        _middlewares.value.forEach { middleware ->
            val context: Context = _factories[middleware]
                ?.let { it as MiddlewareContextFactory<Context, *> }
                ?.let { factory ->
                    with(factory) { middlewareContext.build() } as Context
                } ?: middlewareContext
            _contexts[middleware] = context
            (middleware as? Middleware<Context>)?.onAttached(context)
        }
    }

    /**
     * Detaches all registered middleware instances when the registry is detached
     *
     * @param middlewareContext The context providing information about the middleware state
     */
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

    /**
     * @return a flow of the current middleware context
     * */
    fun context(): Flow<Context?> = middlewareContextFlow

    /**
     * Executes a suspending block whenever the registry is attached
     *
     * @param block The block to execute within the attached context
     */
    suspend fun repeatOnAttached(block: suspend Context.() -> Unit) {
        var currentContextJob: Job? = null
        context().collectLatest { currentContext ->
            currentContextJob?.cancel()
            currentContextJob = mainScope.launch {
                currentContext?.let { it.block() }
            }
        }
    }

    /**
     * Launches a suspending block when the registry is attached
     *
     * @param block The block to execute.
     * @return The launched [Job] instance
     */
    fun launchOnAttached(block: suspend Context.() -> Unit): Job {
        return stateScope.launch {
            repeatOnAttached(block)
        }
    }

    /**
     * Executes a block within the middleware context if available
     *
     * @param block executable context middleware lambda
     * */
    fun <R> execute(block: Context.() -> R): R? {
        return middlewareContext?.block()
    }

    /**
     * @return a list of all middleware instances of type [M]
     * */
    inline fun <reified C : MiddlewareContext, reified M : Middleware<C>> middlewares(): List<M> {
        return middlewares.value.filterIsInstance<M>()
    }

    /**
     * Registers a middleware instance
     *
     * @param middleware a new registered middleware
     */
    fun register(middleware: Middleware<in Context>) = apply {
        _middlewares.update { before ->
            if (before.contains(middleware)) {
                before
            } else {
                val after = before.toMutableSet()
                after.add(middleware)
                middlewareContext?.let { middleware.onAttached(it) }
                after
            }
        }
    }

    /**
     * Registers a middleware instance with a context factory
     *
     * @param middleware a new registered middleware
     * @param factory middleware context factory
     */
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

    /**
     * Unregisters a middleware instance
     *
     * @param middleware the middleware that should be unregistered
     */
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

    /** Unregisters all middleware instances. */
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

    /**
     * Retrieves all middleware instances of a given class type
     *
     * @param clazz unregistered middleware class type
     */
    @Suppress("UNCHECKED_CAST")
    fun <M : Middleware<Context>> getMiddlewares(clazz: KClass<out M>): List<M> {
        return middlewares.value.filter { clazz.isInstance(it) } as List<M>
    }

    /**
     * Retrieves a single middleware instance of a given class type.
     *
     * @param clazz unregistered middleware class type
     */
    @Suppress("UNCHECKED_CAST")
    fun <M : Middleware<Context>> getMiddleware(clazz: KClass<out M>): M? {
        return middlewares.value.firstOrNull { clazz.isInstance(it) } as? M
    }

    /**
     * Observes middleware instances of a given class type.
     *
     * @param clazz middleware class type
     */
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
