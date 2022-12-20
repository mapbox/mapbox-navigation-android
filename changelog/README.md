# Multi-file running changelog

To avoid merge conflicts in the CHANGELOG.md file we accepted the multi-file running changelog strategy.

To follow this strategy you should create a `.md` file for every PR. Choose a directory:

- `changelog/unreleased/features` for **Features** changes
- `changelog/unreleased/bugfixes` for **Bug fixes and improvements** changes
- `changelog/unreleased/issues` for **Known issues :warning:** changes
- `changelog/unreleased/other` for other changes

You can use anything that allow .md format in changelog files.

If you have implemented several features or bugfixes you should describe all of them:

```
- Description of changes in md format
- Description of changes in md format also
```

You can choose any name for your changelog files because the GitHub action will rename files in
`changelog/unreleased/features` and `changelog/unreleased/bugfixes` directories to `${PR_NUMBER}.md` when you open a PR.

Every push to the main or release branch Assemble changelog GitHub action will be executed:

* collect all files from `changelog/unreleased`
* assemble the changelog like:

```
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

* write the changelog to the `changelog/unreleased/CHANGELOG.md` file
