# Welcome

Testing out the Android's benchmarking tools.
https://developer.android.com/jetpack/androidx/releases/benchmark
https://developer.android.com/studio/profile/benchmark
https://developer.android.com/studio/profile/memory-profiler
https://developer.android.com/studio/profile/run-benchmarks-in-ci

## Local development

### Run from command line

Try this, it's fast!

`./gradlew benchmark:connectedCheck`

### Emulator won't work

1. Try setting the test instrumentation runner arguments
``` groovy
android {
    ...
    defaultConfig {
        ...
        testInstrumentationRunnerArgument 'androidx.benchmark.suppressErrors', 'DEBUGGABLE,EMULATOR,LOW_BATTERY,UNLOCKED'
    }
}
```

2. Try running `./gradlew lockClocks` multiple times

The error literally says this, but we don't always read them. Try it!

``` error
java.lang.AssertionError: ERRORS (not suppressed): UNLOCKED
(Suppressed errors: EMULATOR LOW_BATTERY)

WARNING: Unlocked CPU clocks
    Benchmark appears to be running on a rooted device with unlocked CPU
    clocks. Unlocked CPU clocks can lead to inconsistent results due to
    dynamic frequency scaling, and thermal throttling. On a rooted device,
    lock your device clocks to a stable frequency with `./gradlew lockClocks`
```

### Benchmark module is not found

Still investigating
Doesn't really work: Android Studio > File > Sync Project with Gradle Files
Seems like benchmarks need to be a library, and then this module only runs them
