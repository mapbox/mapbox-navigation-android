package com.mapbox.navigation.ui.maneuver

class MockBannerComponents private constructor(
    val type: String,
    val text: String,
    val imageBaseUrl: String?,
    val active: Boolean?,
    val directions: List<String>?,
    val imageUrl: String?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MockBannerComponents

        if (type != other.type) return false
        if (text != other.text) return false
        if (imageBaseUrl != other.imageBaseUrl) return false
        if (active != other.active) return false
        if (directions != other.directions) return false
        if (imageUrl != other.imageUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + (imageBaseUrl?.hashCode() ?: 0)
        result = 31 * result + (active?.hashCode() ?: 0)
        result = 31 * result + (directions?.hashCode() ?: 0)
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "MockBannerComponents(" +
            "type='$type', " +
            "text='$text', " +
            "imageBaseUrl=$imageBaseUrl, " +
            "active=$active, " +
            "directions=$directions, " +
            "imageUrl=$imageUrl" +
            ")"
    }

    class Builder {
        var type: String = "type"
        var text: String = "text"
        var imageBaseUrl: String? = null
        var active: Boolean? = null
        var directions: List<String>? = null
        var imageUrl: String? = null

        fun type(type: String): Builder =
            apply { this.type = type }

        fun text(text: String): Builder =
            apply { this.text = text }

        fun imageBaseUrl(imageBaseUrl: String?): Builder =
            apply { this.imageBaseUrl = imageBaseUrl }

        fun active(active: Boolean?): Builder =
            apply { this.active = active }

        fun directions(directions: List<String>?): Builder =
            apply { this.directions = directions }

        fun imageUrl(imageUrl: String?): Builder =
            apply { this.imageUrl = imageUrl }

        fun build() = MockBannerComponents(
            type,
            text,
            imageBaseUrl,
            active,
            directions,
            imageUrl
        )
    }
}
