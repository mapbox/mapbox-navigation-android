## [Metalava](https://android.googlesource.com/platform/tools/metalava/) setup

Use `$> make 1.0-core-check-api` `$> 1.0-core-update-api` (Core) `$> make 1.0-ui-check-api` `$> 1.0-ui-update-api` (UI) from the [`Makefile`](https://github.com/mapbox/mapbox-navigation-android/blob/master/Makefile) to interact. Make sure to run `$> make 1.0-build-core-release` (Core) and `$> make 1.0-build-ui-release` (UI) first so all deps (`jar`s) are available in the `classpath` and no errors are thrown

#### When releasing

Remember to copy `api/current.txt` files to `api/X.Y.Z.txt` for every module

#### To update `metalava`

```
$> make update-metalava
sh ./scripts/update_metalava.sh
-n Cloning…
 Done
-n Building…
 Done
-e
Dependencies:

com.android.tools.external.org-jetbrains:uast:27.1.0-alpha10
com.android.tools.external.com-intellij:intellij-core:27.1.0-alpha10
com.android.tools.lint:lint-api:27.1.0-alpha10
com.android.tools.lint:lint-checks:27.1.0-alpha10
com.android.tools.lint:lint-gradle:27.1.0-alpha10
com.android.tools.lint:lint:27.1.0-alpha10
org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72
org.jetbrains.kotlin:kotlin-reflect:1.3.72
```

- That places the latest `metalava.jar` into [`metalava`](https://github.com/mapbox/mapbox-navigation-android/blob/master/metalava) folder and prints out the its deps 👀

```groovy
// Metalava isn't released yet. Check in its jar and explicitly track its transitive deps.
```

- Copy and paste (update) the new deps into [`gradle/metalava-dependencies.gradle`](https://github.com/mapbox/mapbox-navigation-android/blob/master/gradle/metalava-dependencies.gradle)