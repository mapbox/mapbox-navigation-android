#!/usr/bin/env bash

set -e
set -o pipefail


# libandroid-navigation

file_path="libandroid-navigation/build/outputs/aar/libandroid-navigation-release.aar"
file_size=$(wc -c <"$file_path" | sed -e 's/^[[:space:]]*//')
date=`date '+%Y-%m-%d'`
utc_iso_date=`date -u +'%Y-%m-%dT%H:%M:%SZ'`
label="Navigation AAR"
source="mobile_binarysize"
scripts_path="scripts"
json_name="$scripts_path/android-binarysize.json"
json_gz="$scripts_path/android-binarysize.json.gz"

# Publish to github
"$scripts_path"/publish_to_sizechecker.js "$file_size" "$label"

# Write binary size to json file
cat >"$json_name" <<EOL
{"sdk": "com.mapbox.services.android.navigation", "platform": "android", "size": ${file_size}, "created_at": "${utc_iso_date}"}
EOL

# Compress json file
gzip -f "$json_name" > "$json_gz"

# Publish to aws
"$scripts_path"/publish_to_aws.sh $source $date $json_gz


# libandroid-navigation-ui

file_path="libandroid-navigation-ui/build/outputs/aar/libandroid-navigation-ui-release.aar"
file_size=$(wc -c <"$file_path" | sed -e 's/^[[:space:]]*//')
date=`date '+%Y-%m-%d'`
utc_iso_date=`date -u +'%Y-%m-%dT%H:%M:%SZ'`
label="Navigation UI AAR"
source="mobile_binarysize"
scripts_path="scripts"
json_name="$scripts_path/android-binarysize.json"
json_gz="$scripts_path/android-binarysize.json.gz"

# Publish to github
"$scripts_path"/publish_to_sizechecker.js "$file_size" "$label"

# Write binary size to json file
cat >"$json_name" <<EOL
{"sdk": "com.mapbox.services.android.navigation.ui", "platform": "android", "size": ${file_size}, "created_at": "${utc_iso_date}"}
EOL

# Compress json file
gzip -f "$json_name" > "$json_gz"

# Publish to aws
"$scripts_path"/publish_to_aws.sh $source $date $json_gz