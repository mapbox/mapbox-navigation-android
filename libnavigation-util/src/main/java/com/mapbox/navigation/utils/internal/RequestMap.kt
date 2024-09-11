package com.mapbox.navigation.utils.internal

class RequestMap<T> {
    private val requestIdGenerator = RequestIdGenerator()
    private val requests = mutableMapOf<Long, T>()

    fun put(value: T): Long {
        val requestId = requestIdGenerator.generateRequestId()
        put(requestId, value)
        return requestId
    }

    fun put(requestId: Long, value: T) {
        requests.put(requestId, value)?.let {
            throw IllegalArgumentException(
                "The request with ID '$requestId' is already in progress.",
            )
        }
    }

    fun get(id: Long): T? = requests[id]

    fun remove(id: Long): T? = requests.remove(id)

    fun removeAll(): List<T> {
        val values = requests.values.toList()
        requests.clear()
        return values
    }

    fun generateNextRequestId() = requestIdGenerator.generateRequestId()
}

private class RequestIdGenerator {
    private var lastRequestId = 0L
    fun generateRequestId(): Long = ++lastRequestId
}

fun <T> RequestMap<T>.cancelRequest(
    requestId: Long,
    tag: String,
    cancellationFn: (T) -> Unit,
) {
    val request = this.remove(requestId)
    if (request != null) {
        cancellationFn(request)
    } else {
        logW(
            "Trying to cancel non-existing route request with id '$requestId'",
            tag,
        )
    }
}
