## Spoken instructions

Turn instructions are announced in the user interface language when turn instructions are available in that language. Otherwise, if turn instructions are unavailable in that language, they are announced in English instead. To have instructions announced in a language other than the user interface language, set the `NavigationRoute.Builder#language` property when calculating the route with which to start navigation.

Turn instructions are primarily designed to be announced by either the Mapbox Voice API (powered by [Amazon Polly](https://docs.aws.amazon.com/polly/latest/dg/SupportedLanguage.html)) or [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech). By default, this SDK uses the Mapbox Voice API, which requires an Internet connection at various points along the route. If the Voice API lacks support for the turn instruction language or there is no Internet connection, TextToSpeech announces the instructions instead. 

By default, distances are given in the predominant measurement system of the system region, which may not necessarily be the same region in which the user is traveling. To override the measurement system used in spoken instructions, set the `MapboxNavigationOptions.Builder#unitType` property when calculating the route with which to start navigation.

The upcoming road or ramp destination is named according to the local or national language. In some regions, the name may be given in multiple languages.

## Supported languages

The table below lists the languages that are supported for user interface elements and for spoken instructions.

| Language   | User interface | [Spoken instructions][apidoc] | Remarks
|------------|:--------------:|:-----------------------------:|--------
| Bengali    | ✅             | —
| Burmese    | ✅             | —
| Chinese    | -              | ✅ Mandarin | Depends on the device; may require third-party text-to-speech
| Czech      | ✅             | -
| Danish     | ✅             | ✅
| English    | ✅             | ✅
| Esperanto  | —              | ✅ 
| French     | ✅             | ✅
| German     | ✅             | ✅
| Hebrew     | ✅             | ✅ | Depends on the device; may require third-party text-to-speech
| Indonesian | —              | ✅ | Depends on the device; may require third-party text-to-speech
| Italian    | ✅             | ✅
| Korean     | ✅             | —
| Portuguese | ✅             | ✅
| Polish     | —              | ✅ 
| Romanian   | —              | ✅ 
| Russian    | ✅             | ✅
| Spanish    | ✅             | ✅
| Swedish    | ✅             | ✅
| Turkish    | —              | ✅ 
| Ukrainian  | ✅              | ✅ | Depends on the device; may require third-party text-to-speech
| Vietnamese | ✅              | ✅ | Depends on the device; may require third-party text-to-speech

**Please note:** For languages marked with `Depends on the device; may require third-party text-to-speech`, instructions are provided by the SDK, but we cannot guarantee the given device will have the appropriate `TextToSpeech` speech engine installed to pronounce these instructions correctly.  

## Contributing

See the [contributing guide](https://github.com/mapbox/mapbox-navigation-ios/blob/master/CONTRIBUTING.md#adding-or-updating-a-localization) for instructions on adding a new localization or improving an existing localization.

[apidoc]: https://www.mapbox.com/api-documentation/#instructions-languages
