package com.mapbox.navigation.testing.utils

import org.junit.Assert.fail

inline fun <reified T> assertNoDiffs(
    expected: T,
    actual: T,
    ignoreGetters: List<String> = emptyList(),
    emptyAndNullCollectionsAreTheSame: Boolean = false,
    doubleComparisonEpsilon: Double = 0.000001,
    nullAndEmptyStringsAreTheSame: Boolean = false,
    nullAndEmptyJsonObjectsAreTheSame: Boolean = false,
    tag: String = "",
) {
    val result = findDiff(
        baseClass = T::class.java,
        expected = expected,
        actual = actual,
        ignoreGetters = ignoreGetters,
        nullAndEmptyCollectionsAreTheSame = emptyAndNullCollectionsAreTheSame,
        doubleComparisonEpsilon = doubleComparisonEpsilon,
        nullAndEmptyStringsAreTheSame = nullAndEmptyStringsAreTheSame,
        nullAndEmptyJsonObjectsAreTheSame = nullAndEmptyJsonObjectsAreTheSame,
    )
    if (result.differences.isNotEmpty()) {
        val tag = if (tag.isNotEmpty()) "$tag: " else ""
        val message = "$tag objects are different:\n" + result.differences.joinToString("\n") {
            "path: ${it.path}, expected: ${it.expectedValueString}, actual: ${it.actualValueString}"
        }
        fail(message)
    }
}

data class FindDiffResult(
    val differences: List<Difference>,
)

data class Difference(
    val path: String,
    val expectedValueString: String,
    val actualValueString: String,
)

// Wrapper class for identity-based equality (uses === instead of ==)
private class IdentityWrapper(val obj: Any) {
    override fun equals(other: Any?): Boolean {
        return other is IdentityWrapper && this.obj === other.obj
    }
    
    override fun hashCode(): Int {
        return System.identityHashCode(obj)
    }
}

fun <T> findDiff(
    baseClass: Class<T>,
    expected: T,
    actual: T,
    ignoreGetters: List<String> = emptyList(),
    nullAndEmptyCollectionsAreTheSame: Boolean = false,
    visitedPathCallback: (String) -> Unit = {},
    doubleComparisonEpsilon: Double = 0.000001,
    nullAndEmptyStringsAreTheSame: Boolean = false,
    nullAndEmptyJsonObjectsAreTheSame: Boolean = false,
): FindDiffResult {
    // Use IdentityHashMap-based set to track visited objects by identity, not equals()
    val visited = mutableSetOf<IdentityWrapper>()
    val differences = findDiffInternal(
        baseClass,
        expected,
        actual,
        "",
        ignoreGetters,
        visited,
        nullAndEmptyCollectionsAreTheSame,
        visitedPathCallback,
        doubleComparisonEpsilon,
        nullAndEmptyStringsAreTheSame,
        nullAndEmptyJsonObjectsAreTheSame,
    )
    return FindDiffResult(differences)
}

@Suppress("UNCHECKED_CAST")
private fun findDiffInternal(
    baseClass: Class<*>,
    expected: Any?,
    actual: Any?,
    pathPrefix: String,
    ignoreGetters: List<String>,
    visited: MutableSet<IdentityWrapper>,
    nullAndEmptyArraysAreTheSame: Boolean,
    visitedPathCallback: (String) -> Unit,
    doubleComparisonEpsilon: Double,
    nullAndEmptyStringsAreTheSame: Boolean,
    nullAndEmptyJsonObjectsAreTheSame: Boolean,
): List<Difference> {
    val differences = mutableListOf<Difference>()
    
    // Handle null cases
    if (expected == null && actual == null) {
        if (pathPrefix.isNotEmpty()) {
            visitedPathCallback(pathPrefix)
        }
        return differences
    }
    if (nullAndEmptyArraysAreTheSame) {
        val expectedIsEmptyCollectionOrArray = isEmptyCollectionOrArray(expected)
        val actualIsEmptyCollectionOrArray = isEmptyCollectionOrArray(actual)
        if ((expected == null && actualIsEmptyCollectionOrArray)
            || (actual == null && expectedIsEmptyCollectionOrArray)
        ) {
            if (pathPrefix.isNotEmpty()) {
                visitedPathCallback(pathPrefix)
            }
            return differences
        }
    }
    if (nullAndEmptyStringsAreTheSame &&
        areNullAndEmptyStringEquivalents(expected, actual)
    ) {
        if (pathPrefix.isNotEmpty()) {
            visitedPathCallback(pathPrefix)
        }
        return differences
    }
    if (expected == null || actual == null) {
        val fieldName = if (pathPrefix.isEmpty()) "root" else pathPrefix
        visitedPathCallback(fieldName)
        differences.add(
            Difference(
                path = fieldName,
                expectedValueString = expected?.toString() ?: "null",
                actualValueString = actual?.toString() ?: "null"
            )
        )
        return differences
    }
    
    // Check for circular references - if we've already visited these objects, skip recursive comparison
    if (visited.contains(IdentityWrapper(expected)) || visited.contains(IdentityWrapper(actual))) {
        return differences
    }
    
    // Handle Collections (List, Set, Map) - compare elements recursively
    if (isCollectionType(expected::class.java) || isCollectionType(actual::class.java)) {
        val fieldName = if (pathPrefix.isEmpty()) "root" else pathPrefix
        
        // If collection types are fundamentally incompatible (e.g., List vs Set), report as difference
        val expectedIsCollection = isCollectionType(expected::class.java)
        val actualIsCollection = isCollectionType(actual::class.java)
        if (!expectedIsCollection || !actualIsCollection) {
            differences.add(
                Difference(
                    path = fieldName,
                    expectedValueString = expected.toString(),
                    actualValueString = actual.toString()
                )
            )
            return differences
        }
        
        // Handle Lists - compare element by element
        if (expected is List<*> && actual is List<*>) {
            val expectedList = expected
            val actualList = actual
            
            // Add the list field name since we're visiting it
            visitedPathCallback(fieldName)
            
            // If sizes differ, report at collection level
            if (expectedList.size != actualList.size) {
                differences.add(
                    Difference(
                        path = fieldName,
                        expectedValueString = "List with ${expectedList.size} elements",
                        actualValueString = "List with ${actualList.size} elements"
                    )
                )
                return differences
            }
            
            // Compare each element
            for (index in expectedList.indices) {
                val expectedElement = expectedList[index]
                val actualElement = actualList[index]
                val elementPath = "$fieldName[$index]"
                
                // Handle null elements
                if (expectedElement == null && actualElement == null) {
                    visitedPathCallback(elementPath)
                    continue
                }
                if (nullAndEmptyStringsAreTheSame &&
                    areNullAndEmptyStringEquivalents(expectedElement, actualElement)
                ) {
                    visitedPathCallback(elementPath)
                    continue
                }
                if (expectedElement == null || actualElement == null) {
                    visitedPathCallback(elementPath)
                    differences.add(
                        Difference(
                            path = elementPath,
                            expectedValueString = expectedElement?.toString() ?: "null",
                            actualValueString = actualElement?.toString() ?: "null"
                        )
                    )
                    continue
                }
                
                // Determine if elements are complex objects or simple types
                val expectedClass = expectedElement::class.java
                val actualClass = actualElement::class.java
                
                // Find common base class to handle different implementations
                val elementType = findCommonSuperclass(expectedClass, actualClass)
                val isComplexElement = !isPrimitiveOrSimpleType(elementType)
                
                if (isComplexElement) {
                    // Recursively compare complex elements to find specific field differences
                    // Use a fresh visited set for each element to avoid false circular reference detection
                    val elementDifferences = findDiffInternal(
                        elementType,
                        expectedElement,
                        actualElement,
                        elementPath,
                        ignoreGetters,
                        mutableSetOf(),  // Fresh visited set for each element
                        nullAndEmptyArraysAreTheSame,
                        visitedPathCallback,
                        doubleComparisonEpsilon,
                        nullAndEmptyStringsAreTheSame,
                        nullAndEmptyJsonObjectsAreTheSame,
                    )
                    differences.addAll(elementDifferences)
                    // Always add the element path since we visited it
                    visitedPathCallback(elementPath)
                } else {
                    // Compare simple elements directly
                    visitedPathCallback(elementPath)
                    if (nullAndEmptyStringsAreTheSame &&
                        areNullAndEmptyStringEquivalents(expectedElement, actualElement)
                    ) {
                        continue
                    }
                    if (!areValuesEqual(
                            expectedElement,
                            actualElement,
                            doubleComparisonEpsilon,
                            nullAndEmptyStringsAreTheSame
                        )) {
                        differences.add(
                            Difference(
                                path = elementPath,
                                expectedValueString = expectedElement.toString(),
                                actualValueString = actualElement.toString()
                            )
                        )
                    }
                }
            }
            return differences
        }
        
        // Handle Maps - compare entry by entry
        if (expected is Map<*, *> && actual is Map<*, *>) {
            val expectedMap = expected
            val actualMap = actual
            
            // Add the map field name since we're visiting it
            visitedPathCallback(fieldName)
            
            // Get all keys from both maps
            val allKeys = (expectedMap.keys + actualMap.keys).toSet()
            
            // Compare each key's value
            for (key in allKeys) {
                val keyString = key.toString()
                val expectedValue = expectedMap[key]
                val actualValue = actualMap[key]
                val entryPath = "$fieldName.$keyString"
                
                // Handle missing keys
                if (!expectedMap.containsKey(key)) {
                    visitedPathCallback(entryPath)
                    differences.add(
                        Difference(
                            path = entryPath,
                            expectedValueString = "null",
                            actualValueString = actualValue?.toString() ?: "null"
                        )
                    )
                    continue
                }
                if (!actualMap.containsKey(key)) {
                    visitedPathCallback(entryPath)
                    differences.add(
                        Difference(
                            path = entryPath,
                            expectedValueString = expectedValue?.toString() ?: "null",
                            actualValueString = "null"
                        )
                    )
                    continue
                }
                
                // Handle null values
                if (expectedValue == null && actualValue == null) {
                    visitedPathCallback(entryPath)
                    continue
                }
                if (nullAndEmptyStringsAreTheSame &&
                    areNullAndEmptyStringEquivalents(expectedValue, actualValue)
                ) {
                    visitedPathCallback(entryPath)
                    continue
                }
                if (expectedValue == null || actualValue == null) {
                    visitedPathCallback(entryPath)
                    differences.add(
                        Difference(
                            path = entryPath,
                            expectedValueString = expectedValue?.toString() ?: "null",
                            actualValueString = actualValue?.toString() ?: "null"
                        )
                    )
                    continue
                }
                
                // Check if this is a Gson JsonElement
                if (isJsonElement(expectedValue) || isJsonElement(actualValue)) {
                    val jsonDifferences = compareJsonElements(
                        expectedValue,
                        actualValue,
                        entryPath,
                        visitedPathCallback,
                        nullAndEmptyArraysAreTheSame,
                        nullAndEmptyJsonObjectsAreTheSame,
                        doubleComparisonEpsilon,
                    )
                    differences.addAll(jsonDifferences)
                    continue
                }
                
                // Determine if values are complex objects or simple types
                val expectedClass = expectedValue::class.java
                val actualClass = actualValue::class.java
                
                // Find common base class to handle different implementations
                val valueType = findCommonSuperclass(expectedClass, actualClass)
                val isComplexValue = !isPrimitiveOrSimpleType(valueType)
                
                if (isComplexValue) {
                    // Recursively compare complex values to find specific field differences
                    val valueDifferences = findDiffInternal(
                        valueType,
                        expectedValue,
                        actualValue,
                        entryPath,
                        ignoreGetters,
                        mutableSetOf(),  // Fresh visited set
                        nullAndEmptyArraysAreTheSame,
                        visitedPathCallback,
                        doubleComparisonEpsilon,
                        nullAndEmptyStringsAreTheSame,
                        nullAndEmptyJsonObjectsAreTheSame,
                    )
                    differences.addAll(valueDifferences)
                    visitedPathCallback(entryPath)
                } else {
                    // Compare simple values directly
                    visitedPathCallback(entryPath)
                    if (nullAndEmptyStringsAreTheSame &&
                        areNullAndEmptyStringEquivalents(expectedValue, actualValue)
                    ) {
                        continue
                    }
                    if (!areValuesEqual(
                            expectedValue,
                            actualValue,
                            doubleComparisonEpsilon,
                            nullAndEmptyStringsAreTheSame
                        )) {
                        differences.add(
                            Difference(
                                path = entryPath,
                                expectedValueString = expectedValue.toString(),
                                actualValueString = actualValue.toString()
                            )
                        )
                    }
                }
            }
            return differences
        }
        
        // For other collections (Set), compare directly using equals
        visitedPathCallback(fieldName)
        val valuesDiffer = !areValuesEqual(
            expected,
            actual,
            doubleComparisonEpsilon,
            nullAndEmptyStringsAreTheSame,
        )
        if (valuesDiffer) {
            differences.add(
                Difference(
                    path = fieldName,
                    expectedValueString = expected.toString(),
                    actualValueString = actual.toString()
                )
            )
        }
        return differences
    }
    
    // Mark objects as visited to prevent circular references
    visited.add(IdentityWrapper(expected))
    visited.add(IdentityWrapper(actual))
    
    // Get all methods from the base class and its parent classes
    val methods = baseClass.methods
    
    // Filter for getter methods:
    // 1. Methods starting with "get" (standard Java getters)
    // 2. Methods without "get" prefix that take no parameters and return a value (AutoValue-style getters)
    // Exclude standard Object methods, builder/helper methods, and Kotlin data class component methods
    val getterMethods = methods.filter { method ->
        method.parameterCount == 0 &&
        method.returnType != Void.TYPE &&
        !isStandardObjectMethod(method) &&
        !isBuilderOrHelperMethod(method) &&
        !isDataClassComponentMethod(method) &&
        (method.name.startsWith("get") || isAutoValueStyleGetter(method))
    }
    
    // Compare values from each getter
    for (getter in getterMethods) {
        getter.isAccessible = true
        
        // Extract field name from getter name
        // For "get" prefix: "getName" -> "name"
        // For AutoValue-style: "waypoints" -> "waypoints"
        val fieldName = if (getter.name.startsWith("get")) {
            getter.name.removePrefix("get").let { name ->
                if (name.isNotEmpty()) {
                    name[0].lowercaseChar() + name.substring(1)
                } else {
                    name
                }
            }
        } else {
            getter.name
        }
        
        val currentPath = if (pathPrefix.isEmpty()) fieldName else "$pathPrefix.$fieldName"
        
        // Skip if this getter is in the ignore list (check both field name and full path)
        if (ignoreGetters.contains(fieldName) || ignoreGetters.contains(currentPath)) {
            continue
        }
        
        // Try to get values from both objects, catching any exceptions
        val expectedResult = try {
            Result.success(getter.invoke(expected))
        } catch (e: Exception) {
            // Unwrap InvocationTargetException to get the original exception
            val originalException = if (e is java.lang.reflect.InvocationTargetException) {
                e.cause ?: e
            } else {
                e
            }
            Result.failure<Any?>(originalException)
        }
        
        val actualResult = try {
            Result.success(getter.invoke(actual))
        } catch (e: Exception) {
            // Unwrap InvocationTargetException to get the original exception
            val originalException = if (e is java.lang.reflect.InvocationTargetException) {
                e.cause ?: e
            } else {
                e
            }
            Result.failure<Any?>(originalException)
        }
        
        // Handle cases where one or both getters throw exceptions
        // Always add the path since we visited it
        visitedPathCallback(currentPath)
        when {
            expectedResult.isFailure && actualResult.isFailure -> {
                // Both threw exceptions - compare the exceptions
                val expectedException = expectedResult.exceptionOrNull()?.toString() ?: "null"
                val actualException = actualResult.exceptionOrNull()?.toString() ?: "null"
                if (expectedException != actualException) {
                    differences.add(
                        Difference(
                            path = currentPath,
                            expectedValueString = expectedException,
                            actualValueString = actualException
                        )
                    )
                }
            }
            expectedResult.isFailure -> {
                // Expected threw exception, actual has value
                val expectedException = expectedResult.exceptionOrNull()?.toString() ?: "null"
                val actualValue = actualResult.getOrNull()
                differences.add(
                    Difference(
                        path = currentPath,
                        expectedValueString = expectedException,
                        actualValueString = actualValue?.toString() ?: "null"
                    )
                )
            }
            actualResult.isFailure -> {
                // Actual threw exception, expected has value
                val expectedValue = expectedResult.getOrNull()
                val actualException = actualResult.exceptionOrNull()?.toString() ?: "null"
                differences.add(
                    Difference(
                        path = currentPath,
                        expectedValueString = expectedValue?.toString() ?: "null",
                        actualValueString = actualException
                    )
                )
            }
            else -> {
                // Both succeeded - proceed with normal comparison
                val expectedValue = expectedResult.getOrNull()
                val actualValue = actualResult.getOrNull()
                
                // Check if this is a complex object that should be recursively compared
                val returnType = getter.returnType
                val isComplexObject = !isPrimitiveOrSimpleType(returnType) &&
                        expectedValue != null && actualValue != null
                
                if (nullAndEmptyArraysAreTheSame &&
                    areNullAndEmptyEquivalents(expectedValue, actualValue)
                ) {
                    continue
                }
                
                if (isComplexObject) {
                    // Recursively compare nested objects
                    val nestedDifferences = findDiffInternal(
                        returnType,
                        expectedValue,
                        actualValue,
                        currentPath,
                        ignoreGetters,
                        visited,
                        nullAndEmptyArraysAreTheSame,
                        visitedPathCallback,
                        doubleComparisonEpsilon,
                        nullAndEmptyStringsAreTheSame,
                        nullAndEmptyJsonObjectsAreTheSame,
                    )
                    differences.addAll(nestedDifferences)
                    // Always add the path since we visited it (already added above)
                } else {
                    // Compare simple values
                val valuesDiffer = !areValuesEqual(
                    expectedValue,
                    actualValue,
                    doubleComparisonEpsilon,
                    nullAndEmptyStringsAreTheSame,
                )
                    
                    if (valuesDiffer) {
                        val expectedValueString = expectedValue?.toString() ?: "null"
                        val actualValueString = actualValue?.toString() ?: "null"
                        
                        differences.add(
                            Difference(
                                path = currentPath,
                                expectedValueString = expectedValueString,
                                actualValueString = actualValueString
                            )
                        )
                    }
                    // Path already added above
                }
            }
        }
    }
    
    return differences
}

private fun isJsonElement(value: Any): Boolean {
    return try {
        val jsonElementClass = Class.forName("com.google.gson.JsonElement")
        jsonElementClass.isInstance(value)
    } catch (e: ClassNotFoundException) {
        false
    }
}

@Suppress("UNCHECKED_CAST")
private fun compareJsonElements(
    expected: Any,
    actual: Any,
    pathPrefix: String,
    visitedPathCallback: (String) -> Unit,
    nullAndEmptyArraysAreTheSame: Boolean = false,
    nullAndEmptyJsonObjectsAreTheSame: Boolean = false,
    doubleComparisonEpsilon: Double = 0.000001,
): List<Difference> {
    val differences = mutableListOf<Difference>()
    
    try {
        val jsonObjectClass = Class.forName("com.google.gson.JsonObject")
        val jsonArrayClass = Class.forName("com.google.gson.JsonArray")
        val jsonPrimitiveClass = Class.forName("com.google.gson.JsonPrimitive")
        val jsonNullClass = Class.forName("com.google.gson.JsonNull")
        
        visitedPathCallback(pathPrefix)
        
        // Handle null and empty array equivalence for JsonElements
        if (nullAndEmptyArraysAreTheSame) {
            val expectedIsEmptyJsonArray = jsonArrayClass.isInstance(expected) && 
                (jsonArrayClass.getMethod("size").invoke(expected) as Int) == 0
            val actualIsEmptyJsonArray = jsonArrayClass.isInstance(actual) && 
                (jsonArrayClass.getMethod("size").invoke(actual) as Int) == 0
            val expectedIsJsonNull = jsonNullClass.isInstance(expected)
            val actualIsJsonNull = jsonNullClass.isInstance(actual)
            
            if ((expectedIsJsonNull && actualIsEmptyJsonArray) || 
                (actualIsJsonNull && expectedIsEmptyJsonArray)) {
                return differences
            }
        }
        
        // Handle null and empty JsonObject equivalence
        if (nullAndEmptyJsonObjectsAreTheSame) {
            val expectedIsEmptyJsonObject = jsonObjectClass.isInstance(expected) && 
                (jsonObjectClass.getMethod("size").invoke(expected) as Int) == 0
            val actualIsEmptyJsonObject = jsonObjectClass.isInstance(actual) && 
                (jsonObjectClass.getMethod("size").invoke(actual) as Int) == 0
            val expectedIsJsonNull = jsonNullClass.isInstance(expected)
            val actualIsJsonNull = jsonNullClass.isInstance(actual)
            
            if ((expectedIsJsonNull && actualIsEmptyJsonObject) || 
                (actualIsJsonNull && expectedIsEmptyJsonObject)) {
                return differences
            }
        }
        
        // Check if types are different
        if (expected::class.java != actual::class.java) {
            differences.add(
                Difference(
                    path = pathPrefix,
                    expectedValueString = expected.toString(),
                    actualValueString = actual.toString()
                )
            )
            return differences
        }
        
        when {
            jsonObjectClass.isInstance(expected) && jsonObjectClass.isInstance(actual) -> {
                // Compare JsonObject
                val expectedObj = expected
                val actualObj = actual
                
                // Get entrySet method
                val entrySetMethod = jsonObjectClass.getMethod("entrySet")
                val expectedEntries = entrySetMethod.invoke(expectedObj) as Set<Map.Entry<String, Any>>
                val actualEntries = entrySetMethod.invoke(actualObj) as Set<Map.Entry<String, Any>>
                
                val expectedMap = expectedEntries.associate { it.key to it.value }
                val actualMap = actualEntries.associate { it.key to it.value }
                
                val allKeys = (expectedMap.keys + actualMap.keys).toSet()
                
                for (key in allKeys) {
                    val expectedValue = expectedMap[key]
                    val actualValue = actualMap[key]
                    val entryPath = "$pathPrefix.$key"
                    
                    if (expectedValue == null && actualValue == null) {
                        visitedPathCallback(entryPath)
                        continue
                    }
                    if (expectedValue == null || actualValue == null) {
                        visitedPathCallback(entryPath)
                        differences.add(
                            Difference(
                                path = entryPath,
                                expectedValueString = expectedValue?.toString() ?: "null",
                                actualValueString = actualValue?.toString() ?: "null"
                            )
                        )
                        continue
                    }
                    
                    // Recursively compare nested JsonElements
                    val nestedDiffs = compareJsonElements(
                        expectedValue,
                        actualValue,
                        entryPath,
                        visitedPathCallback,
                        nullAndEmptyArraysAreTheSame,
                        nullAndEmptyJsonObjectsAreTheSame,
                        doubleComparisonEpsilon,
                    )
                    differences.addAll(nestedDiffs)
                }
            }
            jsonArrayClass.isInstance(expected) && jsonArrayClass.isInstance(actual) -> {
                // Compare JsonArray
                val sizeMethod = jsonArrayClass.getMethod("size")
                val getMethod = jsonArrayClass.getMethod("get", Int::class.java)
                
                val expectedSize = sizeMethod.invoke(expected) as Int
                val actualSize = sizeMethod.invoke(actual) as Int
                
                if (expectedSize != actualSize) {
                    differences.add(
                        Difference(
                            path = pathPrefix,
                            expectedValueString = "JsonArray with $expectedSize elements",
                            actualValueString = "JsonArray with $actualSize elements"
                        )
                    )
                    return differences
                }
                
                for (i in 0 until expectedSize) {
                    val expectedElement = getMethod.invoke(expected, i)!!
                    val actualElement = getMethod.invoke(actual, i)!!
                    val elementPath = "$pathPrefix[$i]"
                    
                    val nestedDiffs = compareJsonElements(
                        expectedElement,
                        actualElement,
                        elementPath,
                        visitedPathCallback,
                        nullAndEmptyArraysAreTheSame,
                        nullAndEmptyJsonObjectsAreTheSame,
                        doubleComparisonEpsilon,
                    )
                    differences.addAll(nestedDiffs)
                }
            }
            jsonPrimitiveClass.isInstance(expected) && jsonPrimitiveClass.isInstance(actual) -> {
                // Compare JsonPrimitive - check if they contain numeric values
                val isNumberMethod = jsonPrimitiveClass.getMethod("isNumber")
                val expectedIsNumber = isNumberMethod.invoke(expected) as Boolean
                val actualIsNumber = isNumberMethod.invoke(actual) as Boolean
                
                if (expectedIsNumber && actualIsNumber) {
                    // Both are numbers - extract as doubles and compare with epsilon
                    val getAsDoubleMethod = jsonPrimitiveClass.getMethod("getAsDouble")
                    val expectedDouble = getAsDoubleMethod.invoke(expected) as Double
                    val actualDouble = getAsDoubleMethod.invoke(actual) as Double
                    
                    if (!areValuesEqual(expectedDouble, actualDouble, doubleComparisonEpsilon, false)) {
                        differences.add(
                            Difference(
                                path = pathPrefix,
                                expectedValueString = expected.toString(),
                                actualValueString = actual.toString()
                            )
                        )
                    }
                } else {
                    // Not both numbers - use string comparison
                    if (expected.toString() != actual.toString()) {
                        differences.add(
                            Difference(
                                path = pathPrefix,
                                expectedValueString = expected.toString(),
                                actualValueString = actual.toString()
                            )
                        )
                    }
                }
            }
            jsonNullClass.isInstance(expected) && jsonNullClass.isInstance(actual) -> {
                // Both are JsonNull - they're equal, do nothing
            }
            else -> {
                // Different types or unexpected JsonElement types
                if (expected.toString() != actual.toString()) {
                    differences.add(
                        Difference(
                            path = pathPrefix,
                            expectedValueString = expected.toString(),
                            actualValueString = actual.toString()
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        // Fallback to string comparison if reflection fails
        visitedPathCallback(pathPrefix)
        if (expected.toString() != actual.toString()) {
            differences.add(
                Difference(
                    path = pathPrefix,
                    expectedValueString = expected.toString(),
                    actualValueString = actual.toString()
                )
            )
        }
    }
    
    return differences
}

private fun areValuesEqual(
    expected: Any?,
    actual: Any?,
    epsilon: Double,
    nullAndEmptyStringsAreTheSame: Boolean,
): Boolean {
    if (expected == null && actual == null) return true
    if (nullAndEmptyStringsAreTheSame &&
        areNullAndEmptyStringEquivalents(expected, actual)
    ) {
        return true
    }
    if (expected == null || actual == null) return false
    
    // Handle Double comparison with epsilon
    if (expected is Double && actual is Double) {
        // Special handling for NaN - treat NaN as equal to NaN
        if (expected.isNaN() && actual.isNaN()) return true
        if (expected.isNaN() || actual.isNaN()) return false
        // Compare with epsilon
        return kotlin.math.abs(expected - actual) <= epsilon
    }
    
    // Handle Float comparison with epsilon
    if (expected is Float && actual is Float) {
        // Special handling for NaN - treat NaN as equal to NaN
        if (expected.isNaN() && actual.isNaN()) return true
        if (expected.isNaN() || actual.isNaN()) return false
        // Compare with epsilon
        return kotlin.math.abs(expected - actual) <= epsilon
    }
    
    // For all other types, use default equality
    return expected == actual
}

private fun isPrimitiveOrSimpleType(type: Class<*>): Boolean {
    return type.isPrimitive ||
            type == String::class.java ||
            type == java.lang.Integer::class.java ||
            type == java.lang.Long::class.java ||
            type == java.lang.Double::class.java ||
            type == java.lang.Float::class.java ||
            type == java.lang.Boolean::class.java ||
            type == java.lang.Byte::class.java ||
            type == java.lang.Short::class.java ||
            type == java.lang.Character::class.java ||
            type == java.util.Date::class.java ||
            type.isEnum
    // Collections are NOT simple types - they need element-by-element comparison
}

private fun isCollectionType(type: Class<*>): Boolean {
    return java.util.Collection::class.java.isAssignableFrom(type) ||
            java.util.Map::class.java.isAssignableFrom(type)
}

private fun isEmptyCollectionOrArray(value: Any?): Boolean {
    if (value == null) {
        return false
    }

    return when {
        value is Collection<*> -> value.isEmpty()
        value is Map<*, *> -> value.isEmpty()
        value::class.java.isArray -> java.lang.reflect.Array.getLength(value) == 0
        else -> false
    }
}

private fun areNullAndEmptyEquivalents(expected: Any?, actual: Any?): Boolean {
    if (expected == null && actual == null) {
        return false
    }

    return (expected == null && isEmptyCollectionOrArray(actual)) ||
            (actual == null && isEmptyCollectionOrArray(expected))
}

private fun areNullAndEmptyStringEquivalents(expected: Any?, actual: Any?): Boolean {
    if (expected == null && actual == null) {
        return false
    }

    return (expected == null && actual is String && actual.isEmpty()) ||
        (actual == null && expected is String && expected.isEmpty())
}

private fun isStandardObjectMethod(method: java.lang.reflect.Method): Boolean {
    // Exclude methods declared in Object class
    if (method.declaringClass == Any::class.java || method.declaringClass == Object::class.java) {
        return true
    }
    // Also exclude specific standard methods by name as a safety check
    val methodName = method.name
    return methodName == "getClass" ||
            methodName == "hashCode" ||
            methodName == "toString" ||
            methodName == "equals" ||
            methodName == "wait" ||
            methodName == "notify" ||
            methodName == "notifyAll" ||
            methodName == "clone" ||
            methodName == "finalize"
}

private fun isBuilderOrHelperMethod(method: java.lang.reflect.Method): Boolean {
    val methodName = method.name
    // Exclude builder methods and Kotlin data class copy method
    return methodName == "toBuilder" ||
            methodName == "builder" ||
            methodName == "fromJson" ||
            methodName == "typeAdapter" ||
            methodName == "updateWithRequestData" ||
            methodName == "autoBuild" ||
            methodName == "build" ||
            methodName == "copy"
}

private fun isDataClassComponentMethod(method: java.lang.reflect.Method): Boolean {
    // Kotlin data classes generate componentN() methods (component1, component2, etc.)
    // These are used for destructuring and should not be treated as getters
    val methodName = method.name
    return methodName.matches(Regex("component\\d+"))
}

private fun findCommonSuperclass(class1: Class<*>, class2: Class<*>): Class<*> {
    // If classes are the same, return it
    if (class1 == class2) {
        return class1
    }
    
    // Build the superclass hierarchy for class1
    val class1Hierarchy = mutableListOf<Class<*>>()
    var current: Class<*>? = class1
    while (current != null && current != Any::class.java) {
        class1Hierarchy.add(current)
        current = current.superclass
    }
    
    // Find the first class in class2's hierarchy that appears in class1's hierarchy
    current = class2
    while (current != null && current != Any::class.java) {
        if (current in class1Hierarchy) {
            return current
        }
        current = current.superclass
    }
    
    // Default to Object/Any if no common superclass found
    return Any::class.java
}

private fun isAutoValueStyleGetter(method: java.lang.reflect.Method): Boolean {
    // AutoValue-style getters are methods that:
    // 1. Don't start with "get"
    // 2. Take no parameters (already checked in filter)
    // 3. Return a value (already checked in filter)
    // 4. Are not builder/helper methods (already checked in filter)
    // 5. Are not standard Object methods (already checked in filter)
    // 6. Are not static methods
    // 7. Don't have void return type (already checked in filter)
    
    val methodName = method.name
    
    // Exclude methods that are clearly not getters
    if (methodName.startsWith("get") ||
        methodName == "hashCode" ||
        methodName == "toString" ||
        methodName == "equals" ||
        java.lang.reflect.Modifier.isStatic(method.modifiers)) {
        return false
    }
    
    // For AutoValue, getters are typically simple method names that match field names
    // They don't have special prefixes or suffixes
    // We'll accept any method that doesn't start with special prefixes
    return !methodName.startsWith("set") &&
           !methodName.startsWith("is") &&
           !methodName.startsWith("has") &&
           !methodName.startsWith("to") &&
           !methodName.startsWith("from") &&
           !methodName.startsWith("create") &&
           !methodName.startsWith("update") &&
           !methodName.startsWith("add") &&
           !methodName.startsWith("remove") &&
           !methodName.startsWith("clear")
}
