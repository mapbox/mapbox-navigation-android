# Contributing

If you have a usage question pertaining to the Mapbox Navigation SDK for Android, or any of our other products, contact us through [our support page](https://www.mapbox.com/contact/).

If you want to contribute code:

1. Ensure that existing [pull requests](https://github.com/mapbox/mapbox-navigation-android/pulls) and [issues](https://github.com/mapbox/mapbox-navigation-android/issues) don’t already cover your contribution or question.

2. Add an `SDK_REGISTRY_TOKEN` gradle property or env variable with a **secret token**. The token needs to have the `DOWNLOADS:READ` scope. You can obtain the token from your [Mapbox Account page](https://account.mapbox.com/access-tokens/).

3. Add your **public token** to `examples/src/main/res/values/mapbox-access-token.xml` to be able to run the test app. You can obtain the token from your [Mapbox Account page](https://account.mapbox.com/access-tokens/).

4. Pull requests are gladly accepted. If there are any changes that developers should be aware of, please update the [change log](CHANGELOG.md)

5. Mapbox uses checkstyle and ktlint to enforce good code standards. Make sure to read the [codestyle setup](./docs/codestyle-setup.md). CI will fail if your PR contains any mistakes.

6. All contributions require acceptance of the Mapbox ToS. For the full license terms, please see the Mapbox Terms of Service at https://www.mapbox.com/legal/tos/.

## Contributor License Agreement

Thank you for contributing to the Mapbox Navigation SDK for Android ("the SDK")! This Contributor License Agreement (“Agreement”) sets out the terms governing any source code, object code, bug fixes, configuration changes, tools, specifications, documentation, data, materials, feedback, information or other works of authorship that you submit, beginning January 25, 2021, in any form and in any manner, to the SDK (https://github.com/mapbox/mapbox-navigation-android) (collectively “Contributions”).

You agree that the following terms apply to all of your Contributions beginning February 8, 2021. Except for the licenses you grant under this Agreement, you retain all of your right, title and interest in and to your Contributions.

**Disclaimer.** To the fullest extent permitted under applicable law, you provide your Contributions on an "as-is" basis, without any warranties or conditions, express or implied, including, without limitation, any implied warranties or conditions of non-infringement, merchantability or fitness for a particular purpose. You have no obligation to provide support for your Contributions, but you may offer support to the extent you desire.


**Copyright License.** You hereby grant, and agree to grant, to Mapbox, Inc. (“Mapbox”) a non-exclusive, perpetual, irrevocable, worldwide, fully-paid, royalty-free, transferable copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, and distribute your Contributions and such derivative works, with the right to sublicense the foregoing rights through multiple tiers of sublicensees.


**Patent License.** To the extent you have or will have patent rights to grant, you hereby grant, and agree to grant, to Mapbox a non-exclusive, perpetual, irrevocable, worldwide, fully-paid, royalty-free, transferable patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer your Contributions, for any patent claims infringed by your Contributions alone or by combination of your Contributions with the SDK, with the right to sublicense these rights through multiple tiers of sublicensees.


**Moral Rights.** To the fullest extent permitted under applicable law, you hereby waive, and agree not to assert, all of your “moral rights” in or relating to your Contributions for the benefit of Mapbox, its assigns, and their respective direct and indirect sublicensees.


**Third Party Content/Rights.** If your Contribution includes or is based on any source code, object code, bug fixes, configuration changes, tools, specifications, documentation, data, materials, feedback, information, or other works of authorship that you did not author (“Third Party Content”), or if you are aware of any third party intellectual property or proprietary rights in your Contribution (“Third Party Rights”), then you agree to include with the submission of your Contribution full details on such Third Party Content and Third Party Rights, including, without limitation, identification of which aspects of your Contribution contain Third Party Content or are associated with Third Party Rights, the owner/author of the Third Party Content and/or Third Party Rights, where you obtained the Third Party Content, and any applicable third party license terms or restrictions for the Third Party Content and/or Third Party Rights. (You need not identify material from the Mapbox Web SDK project as “Third Party Content” to fulfill the obligations in this paragraph.)


**Representations.** You represent that, other than the Third Party Content and Third Party Rights you identify in your Contributions in accordance with this Agreement, you are the sole author of your Contributions and are legally entitled to grant the licenses and waivers in this Agreement. If your Contributions were created in the course of your employment with your past or present employer(s), you represent that such employer(s) has authorized you to make your Contributions on behalf of such employer(s) or such employer(s) has waived all of their right, title or interest in or to your Contributions.


**No Obligation.** You acknowledge that Mapbox is under no obligation to use or incorporate your Contributions into the SDK. Mapbox has sole discretion in deciding whether to use or incorporate your Contributions.


**Disputes.** These Terms are governed by and construed in accordance with the laws of California, without giving effect to any principles of conflicts of law. Any action arising out of or relating to these Terms must be filed in the state or federal courts for San Francisco County, California, USA, and you hereby consent and submit to the exclusive personal jurisdiction and venue of these courts for the purposes of litigating any such action.
.

**Assignment.** You agree that Mapbox may assign this Agreement, and all of its rights, obligations and licenses hereunder.

## Adding or updating a localization

The Mapbox Navigation SDK for Android features several translations contributed through [Transifex](https://www.transifex.com/mapbox/mapbox-navigation-sdk-for-android/). If your language already has a translation, feel free to complete or proofread it. Otherwise, please [request your language](https://www.transifex.com/mapbox/mapbox-navigation-sdk-for-android/) so you can start translating. Note that we’re primarily interested in languages that Android supports as system languages.

While you’re there, please consider also translating the following related projects:

* [OSRM Text Instructions](https://www.transifex.com/project-osrm/osrm-text-instructions/), which the Mapbox Directions API uses to generate textual and verbal turn instructions ([instructions](https://github.com/Project-OSRM/osrm-text-instructions/blob/master/CONTRIBUTING.md#adding-or-updating-a-localization))
* [Mapbox Navigation SDK for iOS](https://www.transifex.com/mapbox/mapbox-navigation-ios/), the analogous library for iOS applications ([instructions](https://github.com/mapbox/mapbox-navigation-ios/blob/main/CONTRIBUTING.md#adding-or-updating-a-localization))
* [Mapbox Maps SDK for Android](https://www.transifex.com/mapbox/mapbox-maps-android/), which is responsible for the map view and minor UI elements such as the Mapbox Telemetry permissions dialog

Once you’ve finished translating the Android navigation SDK into a new language in Transifex, open an issue in this repository asking to pull in your localization. Or do it yourself:

1. _(First time only.)_ Download the [`tx` command line tool](https://docs.transifex.com/client/installing-the-client) and [configure your .transifexrc](https://docs.transifex.com/client/client-configuration).
1. Run `tx pull -a` to fetch translations from Transifex. You can restrict the operation to just the new language using `tx pull -l xyz`, where _xyz_ is the language code.
1. Commit any new files that were added and open a pull request with your changes.
