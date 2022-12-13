# Multi-file running changelog

To avoid merge conflicts in the CHANGELOG.md file we accepted the multi-file running changelog strategy.

To follow this strategy you should create a `changelog/unreleased/${PR_NUMBER}.json` file for every PR like:

```
[
  {
    "type": "Features",
    "title": "Description of changes"
  }
]
```

`type` it is a header of changes in the `CHANGELOG.md`. `title` it is a text of changes

If you have implemented some features or bugfixes you should describe all of them:

```
[
  {
    "type": "Features",
    "title": "Description of changes 1"
  },
  {
    "type": "Bug fixes and improvements",
    "title": "Description of changes 2"
  }
]
```

Every release the release train app will:

* collect all files from `changelog/unreleased`
* assemble the changelog with versions of dependencies like:
```
## Mapbox Navigation SDK 1.1.1 - 13 December, 2022
### Changelog
[Changes between v1.1.0 and v1.1.1](https://github.com/mapbox/mapbox-navigation-android/compare/v1.1.0...v1.1.1)

#### Features
- Feature 1 [#1234](https://github.com/mapbox/mapbox-navigation-android/pull/1234)
- Feature 2 [#2345](https://github.com/mapbox/mapbox-navigation-android/pull/2345)

#### Bug fixes and improvements
- Bugfix 3 [#3456](https://github.com/mapbox/mapbox-navigation-android/pull/3456)
- Bugfix 4 [#4567](https://github.com/mapbox/mapbox-navigation-android/pull/4567)

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.8.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v10.8.0))
- Mapbox Navigation Native `v115.0.1`
- Mapbox Core Common `v23.0.0`
- Mapbox Java `v6.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.8.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
```
* write the changelog to the `CHANGELOG.md` file
* delete all files from `changelog/unreleased`
