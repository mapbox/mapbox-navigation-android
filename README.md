# Generated documentation

This branch publishes the generated documentation for the most recent releases at:

* Core: https://docs.mapbox.com/android/navigation/api/core/X.X.X/
* UI: https://docs.mapbox.com/android/navigation/api/ui/X.X.X/

To learn how to add documentation see: [CONTRIBUTING.md](https://github.com/mapbox/mapbox-navigation-android/blob/master/CONTRIBUTING.md)

In addition to deploying documentation from this repo, you may also need to update version constants in https://github.com/mapbox/android-docs/ and https://github.com/mapbox/help/. Reach out to @mapbox/docs if you have any questions. To learn more about how generated docs work, see: https://github.com/mapbox/documentation/blob/hey-pages/docs/generated-docs.md.

## Formatting Dokka files

For docs generated with Dokka versions < 1.4, there is a small script you can use to add the Mapbox logo, some light styling, and relevant headings. To use it:

0. Make sure you have Node and npm installed on your computer.
1. `cd ./format-script && npm ci`
2. `node ./scripts/format {version} {flavor}` where `version` is an existing version of docs in this repo (ex: `1.0.2`) and `flavor` is one of "ui" or "core".

## Versions

Most versions prior to 1.0.0 are comprised of two main packages: `core` and `ui`. There are corresponding directories in this branch for storing docs for those two packages. Starting with 1.0.0, Mapbox Navigation Android is published as a single package, stored in the `navigation` directory.
