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

/**
 * Abstract test class that each implementation that follows a builder pattern should implement in a unit test.
 *
 * You can read more about the design goals in https://github.com/mapbox/mapbox-navigation-android/issues/2709.
 *
 * A target pattern looks more-or-less like this:
 * ``` kotlin
 * /**
 *  * Description of the RequiredExample. This is a data class because it is responsible for
 *  * transporting data values.
 *  *
 *  * @param required description of what to expect from required
 *  * @param foo description of what to expect from foo
 *  * @param bar description of what to expect from bar
 *  */
 * class Example private constructor(
 *     val required: String, // required
 *     val foo: String, // optional with default value
 *     val bar: Int? // optional without default value
 * ) {
 *     /**
 *      * @return the builder that created the [Example]
 *      */
 *     fun toBuilder() = Builder(required).also {
 *         it.foo(this.foo)
 *         it.bar(this.bar)
 *     }
 *
 *     override fun equals(other: Any?): Boolean {
 *         if (this === other) return true
 *         if (javaClass != other?.javaClass) return false
 *
 *         other as Example
 *
 *         if (required != other.required) return false
 *         if (foo != other.foo) return false
 *         if (bar != other.bar) return false
 *
 *         return true
 *     }
 *
 *     override fun hashCode(): Int {
 *         var result = required.hashCode()
 *         result = 31 * result + foo.hashCode()
 *         result = 31 * result + (bar ?: 0)
 *         return result
 *     }
 *
 *     override fun toString(): String {
 *         return "Example(required='$required', foo='$foo', bar=$bar)"
 *     }
 *
 *     /**
 *      * Description for when to use this builder
 *      *
 *      * @param required does not have a default value
 *      */
 *     class Builder(
 *         private val required: String
 *     ) {
 *         private var foo: String = "default foo value"
 *         private var bar: Int? = null
 *
 *         /**
 *          * @param foo description of what to expect when writing foo
 *          * @return Builder
 *          */
 *         fun foo(foo: String) =
 *             apply { this.foo = foo }
 *
 *         /**
 *          * @param bar description of what to expect when writing bar
 *          * @return Builder
 *          */
 *         fun bar(bar: Int?) =
 *             apply { this.bar = bar }
 *
 *         /**
 *          * Build new instance of [Example]
 *          * @return Example
 *          */
 *         fun build(): Example {
 *             return Example(
 *                 required,
 *                 foo,
 *                 bar
 *             )
 *         }
 *     }
 * }
 * ```
 */
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

    private val toBuilderMethod = implClass.members.find { it.name == "toBuilder" } as? KFunction
        ?: throw RuntimeException("missing toBuilder method")

    private val builderClass = implClass.nestedClasses.find { it.simpleName == "Builder" }
        ?: throw RuntimeException("missing Builder nested class")
    private val requiredFieldNames =
        builderClass.members.filter { it is KProperty && it !is KMutableProperty }.map { it.name }
    private val optionalFieldNames =
        builderClass.members.filter {
            it is KProperty && it is KMutableProperty && it.visibility?.equals(KVisibility.PUBLIC) == true
        }.map { it.name }
    private val buildMethod = builderClass.members.find { it.name == "build" } as? KFunction
        ?: throw RuntimeException("missing Builder.build method")

    /**
     * This method needs to be overridden and HAS TO explicitly add the @Test annotation in the child.
     * This is needed to trick JUnit4 to process the test class if all the actual test cases are in the abstract parent.
     * TODO add a lint rule to detect the missing annotation in the child, or create a new test runner
     */
    @Test
    abstract fun trigger()

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
    fun builder_allOptionalFieldsHaveCorrespondingImplMethod() {
        val implFields = implClass.members.filterIsInstance<KProperty<*>>().filter {
            it !is KMutableProperty
        }.map { it.name }
        optionalFieldNames.forEach {
            assertTrue(
                "builder method should have a corresponding impl field for $it",
                implFields.contains(it)
            )
        }
    }

    @Test
    fun builder_hasNoPublicFields() {
        val publicFields = builderClass.members.filter {
            it is KProperty<*> && it.visibility != KVisibility.PRIVATE
        }
        assertEquals("there should be no public fields", 0, publicFields.size)
    }

    @Test
    fun impl_toString_coversAllFields() {
        val toStringMethod = implClass.members.find { it.name == "toString" } as KFunction
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
        val requiredFieldValues = mutableListOf<Pair<KProperty<*>, Any>>()
        requiredFields.forEachIndexed { index, kProperty ->
            kProperty.isAccessible = true
            requiredFieldValues.add(Pair(kProperty, kProperty.getter.call(builderInstance)!!))
        }

        val optionalFieldValues = mutableListOf<Pair<KProperty<*>, Any>>()
        val optionalFields =
            builderClass.members.filter { it is KProperty && it is KMutableProperty } as List<KProperty<*>>
        optionalFields.forEach { kProperty ->
            kProperty.isAccessible = true
            optionalFieldValues.add(
                Pair(
                    kProperty,
                    kProperty.getter.call(builderInstance) ?: throw RuntimeException("optional value of ${kProperty.name} not provided for equality test")
                )
            )
        }

        optionalFieldValues.forEach { exclude ->
            val field = exclude.first
            val value = exclude.second
            val builderConstructor = builderClass.constructors.first()
            val sortedRequiredFieldValues = mutableListOf<Any>()
            builderConstructor.parameters.forEach { constructorParam ->
                // the reflection used to retrieve the fields returns them in an alphabetical order
                // so this function sorts the argument values to be provided in an order that matches the constructor declaration
                sortedRequiredFieldValues.add(
                    requiredFieldValues.find { it.first.name == constructorParam.name }?.second
                        ?: throw NullPointerException("Your builder constructor argument name probably doesn't match with the field name it's assigned to. " +
                            "Constructor param name is \"${constructorParam.name}\".")
                )
            }
            val newBuilderInstance =
                builderClass.constructors.first().call(*sortedRequiredFieldValues.toTypedArray())
            field.isAccessible = true
            val defaultValue = field.getter.call(newBuilderInstance)
            if (defaultValue == value) {
                throw RuntimeException("Make sure getFilledUpBuilder() provides a unique value for \"${field.name}\". It should not equal \"$defaultValue\".")
            }
            optionalFieldValues.filter { it != exclude }.forEach { fieldValue ->
                (builderClass.members.find { it is KFunction && it.name == fieldValue.first.name }
                    ?: throw RuntimeException("Make sure the \"${fieldValue.first.name}\" field name has a function with identical name."))
                    .call(newBuilderInstance, fieldValue.second)
            }

            val newImplInstance1 = buildMethod.call(newBuilderInstance)!!
            val newImplInstance2 = buildMethod.call(newBuilderInstance)!!
            assertNotEquals(
                "\"${field.name}\" is not included in the $methodName",
                transformation(implInstance),
                transformation(newImplInstance1)
            )
            assertEquals(
                "\"${field.name}\" is not included in the $methodName",
                transformation(newImplInstance1),
                transformation(newImplInstance2)
            )
        }
    }
}
