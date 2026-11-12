#!/usr/bin/env bash
set -euo pipefail

# Generates API docs for the tag checked out at HEAD and uploads them straight to the docs S3 bucket.
# Do not push to the publisher-production branch: a publisher deploy deletes from S3 everything that is not in that branch.

TAG=${1:?"Usage: $0 <tag>, e.g. v2.23.0"}
VERSION="${TAG#v}"
DEST="s3://docs.mapbox.com/android/navigation/v2/api/${VERSION}/"

if ! git tag --points-at HEAD | grep -qx "${TAG}"; then
    echo "HEAD is not at tag ${TAG}"
    exit 1
fi

make javadoc-dokka

aws s3 sync build/kdoc "${DEST}" --exclude "*.html" --cache-control "max-age=120"
aws s3 sync build/kdoc "${DEST}" --exclude "*" --include "*.html" --cache-control "max-age=0"

echo "Published https://docs.mapbox.com/android/navigation/v2/api/${VERSION}/"
