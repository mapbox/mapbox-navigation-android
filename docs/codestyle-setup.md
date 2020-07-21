# Codestyle Setup

Mapbox uses proper codestyle files to enforce the same codestyle throughout the codebase.

## Configuring [Ktlint](https://github.com/pinterest/ktlint) setup

1. On Mac OS or Linux: _brew install ktlint_
2. Inside Project's root directory: _ktlint --android applyToIDEAProject_
(current root directories is _mapbox-navigation-android_)

### Gradle tasks
- _./gradlew ktlint_ - run ktlint to check code-style
- _./gradlew ktlintFormat_ - run ktlint and try to fix code-style issues. Return non-0 if cannot fix all issues

## Configuring checkstyle.

**For Mac**

Go to `Android Studio -> Preferences`

![Android Studio > Preferences](./screenshots/CodeStyle-1.png)

Click on `Code Style`

![Preferences > Code Style](./screenshots/CodeStyle-2.png)

Click on `Settings Icon -> Import Scheme -> Intellij IDEA code style XML`

![Import Scheme > IntelliJ IDEA code style XML](./screenshots/CodeStyle-3.png)

Select `mapbox-java-codestyle.xml` and click on `Open`

![Select mapbox-java-codestyle.xml](./screenshots/CodeStyle-4.png)

Select the checkbox

![Select checkbox](./screenshots/CodeStyle-5.png)

Click Ok