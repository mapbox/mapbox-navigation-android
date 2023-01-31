## [Metalava](https://android.googlesource.com/platform/tools/metalava/) setup

Use `$> make core-check-api` / `$> make core-update-api` (Core) `$> make ui-check-api` / `$> make ui-update-api` (UI) from the [`Makefile`](https://github.com/mapbox/mapbox-navigation-android/blob/main/Makefile) to interact.

```
$> make core-update-api / make core-check-api 
$> make ui-update-api / make ui-check-api
$> make androidauto-update-api / make androidauto-check-api
```

:warning: Noting that we might need to update / push `api/current.txt` files after running `$> make core-update-api` (Core) / `$> make ui-update-api` (UI) if there are changes / errors we're 🆗 with (e.g. `AddedMethod` changes are marked as errors but don't break SemVer) 🚀

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

com.android.tools.external.org-jetbrains:uast:30.3.0-alpha08
com.android.tools.external.com-intellij:kotlin-compiler:30.3.0-alpha08
com.android.tools.external.com-intellij:intellij-core:30.3.0-alpha08
com.android.tools.lint:lint-api:30.3.0-alpha08
com.android.tools.lint:lint-checks:30.3.0-alpha08
com.android.tools.lint:lint-gradle:30.3.0-alpha08
com.android.tools.lint:lint:30.3.0-alpha08
com.android.tools:common:30.3.0-alpha08
com.android.tools:sdk-common:30.3.0-alpha08
com.android.tools:sdklib:30.3.0-alpha08
org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20
org.jetbrains.kotlin:kotlin-reflect:1.6.20
org.ow2.asm:asm:8.0
org.ow2.asm:asm-tree:8.0
com.google.guava:guava:30.1.1-jre
```

- That places the latest `metalava.jar` into [`metalava`](https://github.com/mapbox/mapbox-navigation-android/blob/main/metalava) folder and prints out its deps 👀

```groovy
// Metalava isn't released yet. Check in its jar and explicitly track its transitive deps.
```

- Copy and paste (update) the new deps into [`gradle/metalava-dependencies.gradle`](https://github.com/mapbox/mapbox-navigation-android/blob/main/gradle/scripts/metalava-dependencies.gradle)