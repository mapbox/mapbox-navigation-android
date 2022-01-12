#!/bin/bash

set -eu
set -o pipefail

if [ -z ${1+x} ]; then
    >&2 echo "Error: pass version as argument. For example: $0 1.42.0 [release branch or main by default]"
    exit
fi

VERSION=$1
# Branch from which the release should be made
if [ -z ${2+x} ]; then
    RELEASE_BRANCH=main
else
    RELEASE_BRANCH=$2
fi

SEMVER_REGEX="^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(\\-[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?(\\+[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?$"
if [[ ! $VERSION =~ $SEMVER_REGEX ]] ; then
    >&2 echo "Error: version $VERSION is not SemVer compatible(https://semver.org/)"
    exit
fi

if ! command -v gh &> /dev/null
then
    >&2 echo "Error: gh could not be found. See: https://cli.github.com/manual/"
    exit
fi

if [[ "$(git branch --show-current)" != "${RELEASE_BRANCH}" ]]; then
    >&2 echo "Error: release should be performed from ${RELEASE_BRANCH} branch"
    exit
fi

if [[ "$(git diff)" != "" ]]; then
    >&2 echo "Error: release should be performed from CLEAN (!!!) ${RELEASE_BRANCH} branch"
    exit
fi

# generate release notes

# add changelog
cat >> RELEASENOTES.md <<- EOM
## Changelog

EOM
scripts/changelog.js --compile >> RELEASENOTES.md

# remove unreleased changelogs
rm -rf changelogs/unreleased

cat > RELEASENOTES.md <<- EOM
### Mapbox dependencies
This release depends on, and has been tested with, the following Mapbox dependencies:

EOM
cat ../build/dependencies.md > RELEASENOTES.md

# change CHANGELOG.md

#TMPDIR=$(mktemp -d)
#trap "rm -rf $TMPDIR" EXIT

## extract header from old changelog
#sed '/# Changelog/q' CHANGELOG.md > $TMPDIR/HEADER
## extract old changelog entries
#sed -n '/# Changelog/,$p' CHANGELOG.md | tail -n +2 > $TMPDIR/CURRENTCHANGELOG
#
## update changelog
#cat $TMPDIR/HEADER > CHANGELOG.md
#echo "" >> CHANGELOG.md
#echo "## [$VERSION] - $(date '+%Y-%m-%d')" >> CHANGELOG.md
#echo "" >> CHANGELOG.md
#cat RELEASENOTES.md >> CHANGELOG.md
#cat $TMPDIR/CURRENTCHANGELOG >> CHANGELOG.md

# create release branch
git checkout -b add-changelog-$VERSION
git add changelogs/unreleased #CHANGELOG.md
git commit -m "created changelog for $VERSION"

# disable interactions
gh config set prompt disabled

# create PR
git push --set-upstream origin add-changelog-$VERSION
gh pr create --base ${RELEASE_BRANCH} --title "Release $VERSION" --body "" --reviewer mapbox/navigation-android

# create release
gh release create $VERSION --draft --target ${RELEASE_BRANCH} --notes-file RELEASENOTES.md --title $VERSION

# remove release notes file
rm RELEASENOTES.md

# switch back to the release branch
git checkout ${RELEASE_BRANCH}