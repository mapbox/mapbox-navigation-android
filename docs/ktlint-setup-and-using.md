## [Ktlint](https://github.com/pinterest/ktlint) setup

1. On Mac OS or Linux: _brew install ktlint_
2. Inside Project's root directory: _ktlint --android applyToIDEAProject_
(current root directories is _mapbox-navigation-android_)

### Gradle tasks
- _./gradlew ktlint_ - run ktlint to check code-style
- _./gradlew ktlintFormat_ - run ktlint and try to fix code-style issues. Return non-0 if cannot fix all issues