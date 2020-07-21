<!-- ⚠️ TEMPLATE ⚠️ -->
<!-- Template for GitHub PR descriptions. Use it as a guide on how to describe your work. Feel free to remove any section when you're opening a PR if you think it does not apply for your committed changes. -->

## Description

Please include a brief summary of the change and which issue is fixed (e.g., fixes [#issue](link))

- [ ] I have added any issue links
- [ ] I have added all related labels (`bug`, `feature`, `new API(s)`, `SEMVER`, etc.)
- [ ] I have added the appropriate milestone and project boards

### Goal

Please describe the PR goals. Just the stuff needed to implement the fix / feature and a simple rationale. It could contain many check points if needed

### Implementation

Please include all the relevant things implemented and also rationale, clarifications / disclaimers etc. related to the approach used. It could be as self code companion comments

## Screenshots or Gifs

Please include all the media files to give some context about what's being implemented or fixed. It's not mandatory to upload screenshots or gifs, but for most of the cases it becomes really handy to get into the scope of the feature / bug being fixed and also it's REALLY useful for UI related PRs ![screenshot gif](link)

## Testing

Please describe the manual tests that you ran to verify your changes

- [ ] I have tested locally (including `SNAPSHOT` upstream dependencies if needed) through testapp/demo app and run all activities to avoid regressions
- [ ] I have tested via a test drive, or a simulation/mock location app
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes

## Checklist

- [ ] My code follows the style guidelines of this project
- [ ] I have performed a self-review of my own code
- [ ] I have updated the `CHANGELOG` including this PR
- [ ] We might need to update / push `api/current.txt` files after running `$> make core-update-api` (Core) / `$> make ui-update-api` (UI) if there are changes / errors we're 🆗 with (e.g. `AddedMethod` changes are marked as errors but don't break SemVer) 🚀 If there are SemVer breaking changes add the `SEMVER` label. See [Metalava](https://github.com/mapbox/mapbox-navigation-android/blob/master/docs/metalava.md) docs
<!-- - [ ] I have added an `Activity` example in the test app showing the new feature implemented (where applicable) -->
<!-- - [ ] I have made corresponding changes to the documentation (where applicable) -->
<!-- - [ ] Any changes to strings have been published to our translation tool (where applicable) -->
<!-- - [ ] Publish `testapp` in Google Play `internal` test track (where applicable) -->