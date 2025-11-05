package com.mapbox.navigation.testing.ui

/**
 * Annotation to mark test classes that take a long time to run.
 *
 * Tests annotated with this marker will be distributed evenly across test suites
 * before other tests to ensure better parallelization and test execution time balance.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LongTest
