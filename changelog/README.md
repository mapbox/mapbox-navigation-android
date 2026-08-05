# Multi-file running changelog

To avoid merge conflicts in the CHANGELOG.md file we accepted the multi-file running changelog strategy.

*This strategy works for the libnavui-androidauto project too. It works in the `libnavui-androidauto/changelog` directory*

To follow this strategy you should create a `.md` file for every PR. Choose a directory:

- `changelog/features` for **Features** changes
- `changelog/bugfixes` for **Bug fixes and improvements** changes
- `changelog/issues` for **Known issues :warning:** changes
- `changelog/other` for other changes

Create the file directly in the appropriate directory. Do not use `scripts/changelog/add_changelog.py`: it writes to the legacy `changelog/unreleased` location, which is not read by the CD release job.

You can use anything that allow .md format in changelog files.

If you have implemented several features or bugfixes you should describe all of them:

```
- Description of changes in md format
- Description of changes in md format also
```

You can choose any name for your changelog files because the GitHub action will rename files in
`changelog/features` and `changelog/bugfixes` directories to `${PR_NUMBER}.md` when you open a PR.

For every PR the script will generate and update a comment with a changelog for the current branch in the following format:

```
# Changelog
#### Features
- Feature 1 [#1234](https://github.com/mapbox/mapbox-navigation-android/pull/1234)
- Feature 2 [#2345](https://github.com/mapbox/mapbox-navigation-android/pull/2345)

#### Bug fixes and improvements
- Bugfix 3 [#3456](https://github.com/mapbox/mapbox-navigation-android/pull/3456)
- Bugfix 4 [#4567](https://github.com/mapbox/mapbox-navigation-android/pull/4567)

#### Known issues :warning:
- Issue 1
- Issue 2

Some other changes
```

The comment will be updated with every change.
Also, a comment with a changelog will be generated and updated for the android auto project too.

Every release the release train app will:

* assemble the changelog
* add information about dependencies and compile changelog like:
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

#### Known issues :warning:
- Issue 1
- Issue 2

Some other changes

### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:
- Mapbox Maps SDK `v10.8.0` ([release notes](https://github.com/mapbox/mapbox-maps-android/releases/tag/v10.8.0))
- Mapbox Navigation Native `v115.0.1`
- Mapbox Core Common `v23.0.0`
- Mapbox Java `v6.8.0` ([release notes](https://github.com/mapbox/mapbox-java/releases/tag/v6.8.0))
- Mapbox Android Core `v5.0.2` ([release notes](https://github.com/mapbox/mapbox-events-android/releases/tag/core-5.0.2))
```
* add compiled changelog to `CHANGELOG.md` file
* delete all files in `changelog/unreleased` dir
