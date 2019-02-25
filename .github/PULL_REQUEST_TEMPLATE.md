### ⚠️ TEMPLATE ⚠️
**Template for GitHub PR descriptions. Use it as a guide on how to describe your work. Feel free to remove any section when you're opening a PR if you think it does not apply for your commited changes.**

## Title

Please include some human-understandable summary

## Description

Please include a brief summary of the change and which issue is fixed

- Fixes [#issue](link)

## What's the goal?

Please describe the PR goals. Just the stuff needed to implement the fix / feature and a simple rationale. It could contain many check points if needed

## How is it being implemented?

Please include all the relevant things implemented and also rationale, aclarations / disclaimers etc. related to the approach used. It could be as self code companion comments

## Screenshots or Gifs

Please include all the media files to give some context about what's being implemented or fixed. It's not mandatory to upload screenshots or gifs, but for most of the cases it becomes really handy to get into the scope of the feature / bug being fixed and aslo it's REALLY useful for UI related PRs

![screenshot gif](link)

## How has this been tested?

Please describe the manual tests that you ran to verify your changes. Test drive if possible, if not use simulation or any mock location app

- Test A
- Test B

## Type of change

Please add all related Labels `bug`, `feature`, `new API(s)`, `SEMVER`, etc.

## Projects

Please attach the ticket to the appropriate project boards

## Milestone

Please add the appropriate milestone to the ticket

## Checklist

- [ ] I have tested locally / staging (including `SNAPSHOT` upstream dependencies if needed)
- [ ] My code follows the style guidelines of this project
- [ ] I have performed a self-review of my own code
- [ ] I have made corresponding changes to the documentation
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes
- [ ] I have added an `Activity` example in the test app showing the new feature implemented
- [ ] Any changes to strings have been published to our translation tool
- [ ] Publish `testapp` in Google Play `internal` test track