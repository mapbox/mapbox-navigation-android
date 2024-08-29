package com.mapbox.navigation.base.options

/**
 * Electronic Horizon supports live incidents on a most probable path.
 * To enable live incidents [IncidentsOptions] should be provided.
 * If both [graph] and [apiUrl] are empty, live incidents are disabled (by default).
 *
 * @param graph incidents provider graph name
 * @param apiUrl LTS incidents service API url, if empty line is supplied will use default url
 */
class IncidentsOptions private constructor(
    val graph: String,
    val apiUrl: String,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        graph(graph)
        apiUrl(apiUrl)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IncidentsOptions

        if (graph != other.graph) return false
        if (apiUrl != other.apiUrl) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = graph.hashCode()
        result = 31 * result + apiUrl.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "IncidentsOptions(" +
            "graph='$graph', " +
            "apiUrl='$apiUrl'" +
            ")"
    }

    /**
     * Build a new [IncidentsOptions]
     */
    class Builder {
        private var graph: String = EMPTY_STRING
        private var apiUrl: String = EMPTY_STRING

        /**
         * Override incidents provider graph name.
         */
        fun graph(graph: String): Builder = apply {
            this.graph = graph
        }

        /**
         * Override LTS incidents service API url.
         */
        fun apiUrl(apiUrl: String): Builder = apply {
            this.apiUrl = apiUrl
        }

        /**
         * Build the [IncidentsOptions]
         */
        fun build(): IncidentsOptions {
            return IncidentsOptions(
                graph = graph,
                apiUrl = apiUrl,
            )
        }
    }

    private companion object {
        private const val EMPTY_STRING = ""
    }
}
