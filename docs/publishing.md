### Artifact distribution
This project uses SDK Registry to distribute the binaries.

1. Push an artifact:
- Cut and push a new tag. It will publish a new artifact to `https://api.mapbox.com/downloads/v2/releases/maven`. The tag has to match the following pattern:
  - `/^release_core_.{VERSION_CORE}/` for Navigation Core SDK
  - `/^release_ui_.{VERSION_UI}_core_.{VERSION_CORE}/` for Navigation UI SDK
- Each merge to `master` pushes new snapshots to `https://api.mapbox.com/downloads/v2/snapshots/maven`. To build a snapshot from a custom branch, modify the `branches` filter of the `release-*-snapshot` CI job.

2. (**only for stable artifacts, skip for snapshots**) Go to https://github.com/mapbox/api-downloads and create a new PR that adds/modifies a `config/mobile-navigation-{artifact}}/{VERSION}.yaml` file. The file should contain this Android entry (next to anything that might be present for iOS):
```yaml
api-downloads: v2

android:
  group: navigation

packages:
  android:
    - {artifactId}

bundles:
  android: {artifactId}-all
```

For example, the version 1.0.0 of the `core` artifact would need a `config/mobile-navigation-core/1.0.0.yaml`:
```yaml
api-downloads: v2

android:
  group: navigation

packages:
  android:
    - core

bundles:
  android: core-all
```

This procedure has to be repeated for all Core and UI artifacts.

After a PR is merged, an artifact will be resolvable.

You can read more about the publishing process at https://platform.mapbox.com/managed-infrastructure/sdk-registry/reference/#configure-the-sdk-registry.

### Consuming a stable artifact using Gradle
```groovy
allprojects {
   repositories {
     maven {
       url 'https://api.mapbox.com/downloads/v2/releases/maven'
       authentication {
         basic(BasicAuthentication)
       }
       credentials {
         username = "mapbox"
         password = "{secret Mapbox token with DOWNLOADS:READ scope}"
       }
     }
   }
}

dependencies {
   implementation 'com.mapbox.navigation:core:VERSION'
   implementation 'com.mapbox.navigation:ui-v1:VERSION'
}
```

### Consuming a snapshot artifact using Gradle
Similarly as above with a difference that the repository URL should be `https://api.mapbox.com/downloads/v2/snapshots/maven`.