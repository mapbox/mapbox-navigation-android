package com.mapbox.navigation.base.model.route

data class RouteResponse(
    private var code: String,
    private var message: String? = null,
    private var routes: List<Route>,
    private var uuid: String? = null
) {

    fun code(): String = code

    fun message(): String? = message

    fun routes(): List<Route> = routes

    fun uuid(): String? = uuid

    class Builder {
        private lateinit var responseCode: String
        private lateinit var responseRoutes: List<Route>
        private var message: String? = null
        private var uuid: String? = null

        fun code(code: String) =
                apply { this.responseCode = code }

        fun routes(routes: List<Route>) =
                apply { this.responseRoutes = routes }

        fun message(message: String) =
                apply { this.message = message }

        fun uuid(uuid: String) =
                apply { this.uuid = uuid }

        fun build(): RouteResponse {
            check(::responseCode.isInitialized) { "Missing property responseCode" }
            check(::responseRoutes.isInitialized) { "Missing property responseRoutes" }
            return RouteResponse(responseCode, message, responseRoutes, uuid)
        }
    }
}
