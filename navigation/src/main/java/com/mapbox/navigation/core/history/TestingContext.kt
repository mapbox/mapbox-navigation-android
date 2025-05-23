package com.mapbox.navigation.core.history

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * A structure to hold testing-related context information.
 *
 * The [TestingContext] structure is designed to provide runtime information
 * about the test vehicle and project for testing purposes.
 * This structure is intended strictly for testing and debugging purposes.
 * Including sensitive information such as VINs or internal project names in
 * production environments is not recommended.
 *
 * @property projectName
 * @property vehicleName
 */
@ExperimentalPreviewMapboxNavigationAPI
class TestingContext private constructor(
    val projectName: String,
    val vehicleName: String,
) {

    /**
     * @return the [Builder] that created the [TestingContext]
     */
    fun toBuilder(): Builder {
        return Builder()
            .projectAndVehicleName(projectName, vehicleName)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestingContext

        if (vehicleName != other.vehicleName) return false
        return projectName == other.projectName
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = vehicleName.hashCode()
        result = 31 * result + projectName.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TestingContext(" +
            "projectName=$projectName, " +
            "vehicleName=$vehicleName" +
            ")"
    }

    /**
     * Builder of [TestingContext]
     */
    class Builder {

        private var vehicleName: String = ""
        private var projectName: String = ""

        /**
         * Project and vehicle name.
         *
         * @param projectName name/area associated with the test
         * @param vehicleName name of the vehicle
         */
        fun projectAndVehicleName(projectName: String, vehicleName: String): Builder = apply {
            this.projectName = projectName
            this.vehicleName = vehicleName
        }

        /**
         * Build a new instance of [TestingContext]
         *
         * @return TestingContext
         */
        fun build(): TestingContext {
            return TestingContext(
                projectName = projectName,
                vehicleName = vehicleName,
            )
        }
    }

    internal companion object {

        @JvmSynthetic
        fun toNativeObject(testingContext: TestingContext) =
            com.mapbox.navigator.TestingContext(
                testingContext.vehicleName,
                testingContext.projectName,
            )
    }
}
