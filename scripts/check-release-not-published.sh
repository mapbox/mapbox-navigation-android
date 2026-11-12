#!/bin/bash -e

# Fails if any SDK in the staged S3 copy commands is already published in api-downloads,
# so a rerun of the upload can't overwrite artifacts that customers can already download.
# A version counts as published when config/<sdk>/<version>.yaml exists in mapbox/api-downloads.

COMMANDS_FILE=$1

if [ ! -f "$COMMANDS_FILE" ]; then
  echo "Commands file not found: $COMMANDS_FILE"
  exit 1
fi

if [ -z "$GITHUB_TOKEN" ]; then
  echo "GITHUB_TOKEN env variable has to be set"
  exit 1
fi

# s3://<bucket>/v2/<sdk>/releases/android/<version> -> "<sdk> <version>"
SDK_VERSIONS=$(grep -oE 's3://[^ ]+/v2/[^/ ]+/releases/android/[^/ ]+' "$COMMANDS_FILE" \
  | sed -E 's|.*/v2/([^/]+)/releases/android/([^/]+)$|\1 \2|' \
  | sort -u)

if [ -z "$SDK_VERSIONS" ]; then
  echo "No release uploads found in $COMMANDS_FILE"
  exit 1
fi

PUBLISHED=0
while read -r SDK VERSION; do
  URL="https://api.github.com/repos/mapbox/api-downloads/contents/config/$SDK/$VERSION.yaml"
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $GITHUB_TOKEN" "$URL")
  case "$STATUS" in
    404) echo "Not published: $SDK $VERSION" ;;
    200) echo "Already published: $SDK $VERSION"; PUBLISHED=1 ;;
    *) echo "Could not check $SDK $VERSION (HTTP $STATUS)"; exit 1 ;;
  esac
done <<< "$SDK_VERSIONS"

if [ "$PUBLISHED" -ne 0 ]; then
  echo "Refusing to upload: the release is already published in api-downloads."
  exit 1
fi
