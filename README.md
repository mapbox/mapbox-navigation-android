# Generated documentation

This branch publishes the generated documentation for the most recent releases at `https://docs.mapbox.com/android/navigation/api/X.X.X/`.

To learn how to add documentation see: [CONTRIBUTING.md](https://github.com/mapbox/mapbox-navigation-android/blob/main/CONTRIBUTING.md)

In addition to deploying documentation from this repo, you may also need to update version constants in https://github.com/mapbox/android-docs/ and https://github.com/mapbox/help/. Reach out to @mapbox/docs if you have any questions. To learn more about how generated docs work, see: https://github.com/mapbox/documentation/blob/hey-pages/docs/generated-docs.md.

## Versions

Most versions prior to 1.0.0 are comprised of two main packages: `core` and `ui`. There are corresponding directories in this branch for storing docs for those two packages. Starting with 1.0.0, Mapbox Navigation Android is published as a single package, stored in folders corresponding to that packages version number. **Do not add generated docs for versions >=1.0.0 to the core/ or ui/ folders.**

```
mapbox-navigation-android

├── 1.0.0
├── 1.1.0
├── 1.2.0
├── x.x.x
├── core
└── ui
```
