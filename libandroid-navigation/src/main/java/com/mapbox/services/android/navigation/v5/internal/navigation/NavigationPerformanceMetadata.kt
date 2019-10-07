package com.mapbox.services.android.navigation.v5.internal.navigation

// Fixme: Remove Builder and add default values to constructor when
// Fixme: NavigationTelemetry and NavigationMetricsWrapper converted to Kotlin
internal data class NavigationPerformanceMetadata(
    val version: String,
    val screenSize: String,
    val country: String,
    val device: String,
    val abi: String,
    val brand: String,
    val ram: String,
    val os: String,
    val gpu: String,
    val manufacturer: String
) {

    companion object {
        @JvmStatic
        internal fun builder() = Builder()

        class Builder internal constructor() {
            private var version = ""
            private var screenSize = ""
            private var country = ""
            private var device = ""
            private var abi = ""
            private var brand = ""
            private var ram = ""
            private var os = ""
            private var gpu = ""
            private var manufacturer = ""

            fun version(version: String) = apply { this.version = version }
            fun screenSize(screenSize: String) = apply { this.screenSize = screenSize }
            fun country(country: String) = apply { this.country = country }
            fun device(device: String) = apply { this.device = device }
            fun abi(abi: String) = apply { this.abi = abi }
            fun brand(brand: String) = apply { this.brand = brand }
            fun ram(ram: String) = apply { this.ram = ram }
            fun os(os: String) = apply { this.os = os }
            fun gpu(gpu: String) = apply { this.gpu = gpu }
            fun manufacturer(manufacturer: String) = apply { this.manufacturer = manufacturer }

            fun build() = NavigationPerformanceMetadata(
                version,
                screenSize,
                country,
                device,
                abi,
                brand,
                ram,
                os,
                gpu,
                manufacturer
            )
        }
    }
}
