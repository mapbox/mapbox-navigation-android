# Contributing

If you have a usage question pertaining to the Mapbox Navigation SDK for Android, or any of our other products, contact us through [our support page](https://www.mapbox.com/contact/).

If you want to contribute code:

1. Please familiarize yourself with the [install process](https://www.mapbox.com/android-docs/navigation/overview/#install-the-navigation-sdk).

2. Ensure that existing [pull requests](https://github.com/mapbox/mapbox-navigation-android/pulls) and [issues](https://github.com/mapbox/mapbox-navigation-android/issues) don’t already cover your contribution or question.

3. Pull requests are gladly accepted. If there are any changes that developers should be aware of, please update the [change log](CHANGELOG.md)

4. Mapbox uses checkstyle to enforce good Java code standards, Make sure to read the [wiki entry](https://github.com/mapbox/mapbox-navigation-android/wiki/Setting-up-Mapbox-checkstyle) and setup. CI will fail if your PR contains any mistakes.

## Adding or updating a localization

The Mapbox Navigation SDK for Android features several translations contributed through [Transifex](https://www.transifex.com/mapbox/mapbox-navigation-sdk-for-android/). If your language already has a translation, feel free to complete or proofread it. Otherwise, please [request your language](https://www.transifex.com/mapbox/mapbox-navigation-sdk-for-android/) so you can start translating. Note that we’re primarily interested in languages that Android supports as system languages.

While you’re there, please consider also translating the following related projects:

* [OSRM Text Instructions](https://www.transifex.com/project-osrm/osrm-text-instructions/), which the Mapbox Directions API uses to generate textual and verbal turn instructions ([instructions](https://github.com/Project-OSRM/osrm-text-instructions/blob/master/CONTRIBUTING.md#adding-or-updating-a-localization))
* [Mapbox Navigation SDK for iOS](https://www.transifex.com/mapbox/mapbox-navigation-ios/), the analogous library for iOS applications ([instructions](https://github.com/mapbox/mapbox-navigation-ios/blob/master/CONTRIBUTING.md#adding-or-updating-a-localization))
* [Mapbox Maps SDK for Android](https://www.transifex.com/mapbox/mapbox-gl-native/), which is responsible for the map view and minor UI elements such as the Mapbox Telemetry permissions dialog

Once you’ve finished translating the Android navigation SDK into a new language in Transifex, open an issue in this repository asking to pull in your localization. Or do it yourself:

1. _(First time only.)_ Download the [`tx` command line tool](https://docs.transifex.com/client/installing-the-client) and [configure your .transifexrc](https://docs.transifex.com/client/client-configuration).
1. Run `tx pull -a` to fetch translations from Transifex. You can restrict the operation to just the new language using `tx pull -l xyz`, where _xyz_ is the language code.
1. Commit any new files that were added and open a pull request with your changes.

# Code of conduct

Everyone is invited to participate in Mapbox’s open source projects and public discussions: we want to create a welcoming and friendly environment. Harassment of participants or other unethical and unprofessional behavior will not be tolerated in our spaces. The [Contributor Covenant](http://contributor-covenant.org) applies to all projects under the Mapbox organization and we ask that you please read [the full text](http://contributor-covenant.org/version/1/2/0/).

You can learn more about our open source philosophy on [mapbox.com](https://www.mapbox.com/about/open/).
