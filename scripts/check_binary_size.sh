#!/usr/bin/env bash

set -e
set -o pipefail

# Argument 1 is a file including file paths (one per line)
paths_file=$1
# Argument 2 is a file including labels (one per line) - Must match paths_file number of lines / size
labels_file=$2
# Argument 3 is the repo name
repo_name=$3
# Argument 4 is the SDK
sdk=$4
# Argument 5 is a file including the platforms (one per line) - Must match paths_file number of lines / size
platforms_file=$5

source=mobile.binarysize
scripts_path="scripts"
json_name="$scripts_path/${sdk}-binarysize.json"
json_gz="$scripts_path/${sdk}-binarysize.json.gz"

date=`date '+%Y-%m-%d'`
utc_iso_date=`date -u +'%Y-%m-%dT%H:%M:%SZ'`

while read label
do
    labels+=("$label")
done <"$labels_file"

# Publish to github
i=0
while read path
do
    file_size=$(wc -c <"$path" | sed -e 's/^[[:space:]]*//')
	"$scripts_path"/publish_to_sizechecker.js "${file_size}" "${labels[${i}]}" "$repo_name"
	i=$(($i + 1))
done <"$paths_file"

while read platform
do
    platforms+=("$platform")
done <"$platforms_file"

# Write binary size to json file
i=0
while read path
do
    file_size=$(wc -c <"$path" | sed -e 's/^[[:space:]]*//')
	echo "{"sdk": ${sdk}, "platform": ${platforms[${i}]}, "size": ${file_size}, "created_at": "${utc_iso_date}"}" >> "$json_name"
	i=$(($i + 1))
done <"$paths_file"

# Compress json file
gzip -f "$json_name" > "$json_gz"

# Publish to aws
"$scripts_path"/publish_to_aws.sh $source $date $json_gz