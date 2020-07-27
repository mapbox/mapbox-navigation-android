package com.mapbox.navigation.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.isAccessible

abstract class BuilderTest<Implementation : Any, Builder> {

    private val implClass: KClass<*> by lazy { getImplementationClass() }

    /**
     * Your builder's resulting type.
     */
    abstract fun getImplementationClass(): KClass<Implementation>

    /**
     * Make sure that all of the required and optional values are provided and that they distinct from default ones.
     */
    abstract fun getFilledUpBuilder(): Builder

    private val toBuilderMethod = implClass.members.find { it.name == "toBuilder" } as KFunction
    private val toStringMethod = implClass.members.find { it.name == "toString" } as KFunction

    private val builderClass = implClass.nestedClasses.find { it.simpleName == "Builder" }!!
    private val requiredFieldNames =
        builderClass.members.filter { it is KProperty && it !is KMutableProperty }.map { it.name }
    private val optionalFieldNames =
        builderClass.members.filter { it is KProperty && it is KMutableProperty }.map { it.name }
    private val buildMethod = builderClass.members.find { it.name == "build" } as KFunction

    @Test
    fun isNotDataClass() {
        assertFalse(implClass.isData)
        assertFalse(builderClass.isData)
    }

    @Test
    fun impl_onlyOneConstructor() {
        assertEquals(1, implClass.constructors.size)
    }

    @Test
    fun builder_onlyOneConstructor() {
        assertEquals(1, builderClass.constructors.size)
    }

    @Test
    fun impl_allConstructorsArePrivate() {
        assertTrue(implClass.constructors.all { it.visibility == KVisibility.PRIVATE })
    }

    @Test
    fun impl_allFieldsAreVals() {
        assertTrue(
            implClass.members.filterIsInstance<KProperty<*>>().all { it !is KMutableProperty }
        )
    }

    @Test
    fun builder_hasNoPublicFields() {
        val publicFields = builderClass.members.filter {
            it is KProperty<*> && it.visibility != KVisibility.PRIVATE
        }
        assertEquals("there should be no public fields", 0, publicFields.size)
    }

    @Test
    fun builderAndImpl_fieldCountIsTheSame() {
        assertEquals(
            "number of fields in a builder should match number of fields in the implementation",
            implClass.members.filterIsInstance<KProperty<*>>().size,
            (requiredFieldNames.size + optionalFieldNames.size)
        )
    }

    @Test
    fun impl_toString_coversAllFields() {
        val builderInstance = getFilledUpBuilder()
        val implInstance = buildMethod.call(builderInstance)
        val string = toStringMethod.call(implInstance) as String
        (requiredFieldNames + optionalFieldNames).forEach {
            assertTrue("$it is not included in the toString method", string.contains(it))
        }
    }

    @Test
    fun toBuilder_createsNewInstance() {
        val builderInstance = getFilledUpBuilder()
        val implInstance = buildMethod.call(builderInstance)
        val retrievedBuilderInstance = toBuilderMethod.call(implInstance)
        assertTrue(
            "Retrieved builder instance is the same as the original one. The builder should be recreated.",
            builderInstance !== retrievedBuilderInstance
        )
    }

    @Test
    fun toBuilder_rebuiltIsEqual() {
        val builderInstance = getFilledUpBuilder()
        val implInstance = buildMethod.call(builderInstance)
        val retrievedBuilderInstance = toBuilderMethod.call(implInstance)
        val rebuiltImplInstance = buildMethod.call(retrievedBuilderInstance)
        assertTrue(
            "Retrieved builder should create a new instance of the implementation.",
            implInstance !== rebuiltImplInstance
        )
        assertEquals(
            "New instance should be equal to the original one",
            implInstance,
            rebuiltImplInstance
        )
    }

    @Test
    fun equalsTest() {
        compareOriginalNotEqualToRebuilt("equals") { instance ->
            return@compareOriginalNotEqualToRebuilt instance
        }
    }

    @Test
    fun hashCodeTest() {
        compareOriginalNotEqualToRebuilt("hashCode") { instance ->
            return@compareOriginalNotEqualToRebuilt instance.hashCode()
        }
    }

    private fun compareOriginalNotEqualToRebuilt(
        methodName: String,
        transformation: (instance: Any) -> Any
    ) {
        val builderInstance = getFilledUpBuilder()
        val implInstance = buildMethod.call(builderInstance)!!

        val requiredFields =
            builderClass.members.filter { it is KProperty && it !is KMutableProperty } as List<KProperty<*>>
        val requiredValues = mutableListOf<Any>()
        requiredFields.forEachIndexed { index, kProperty ->
            kProperty.isAccessible = true
            requiredValues.add(kProperty.getter.call(builderInstance)!!)
        }

        val optionalFieldValues = mutableListOf<Pair<KProperty<*>, Any>>()
        (builderClass.members.filter { it is KProperty && it is KMutableProperty } as List<KProperty<*>>).forEachIndexed { index, kProperty ->
            kProperty.isAccessible = true
            optionalFieldValues.add(Pair(kProperty, kProperty.getter.call(builderInstance)!!))
        }

        optionalFieldValues.forEach { exclude ->
            val newBuilderInstance =
                builderClass.constructors.first().call(*requiredValues.toTypedArray())
            exclude.first.isAccessible = true
            val defaultValue = exclude.first.getter.call(newBuilderInstance)
            if (defaultValue == exclude.second) {
                throw RuntimeException("make sure the provided value is different than default for \"${exclude.first.name}\"")
            }
            optionalFieldValues.filter { it != exclude }.forEach { fieldValue ->
                builderClass.members.find { it is KFunction && it.name == fieldValue.first.name }!!
                    .call(newBuilderInstance, fieldValue.second)
            }
            assertNotEquals(
                "\"${exclude.first.name}\" is not included in the $methodName",
                transformation(implInstance),
                transformation(buildMethod.call(newBuilderInstance)!!)
            )
        }
    }
}
