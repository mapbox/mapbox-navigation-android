#!/bin/bash

declare -A moduleToStorageIdMap
moduleToStorageIdMap["libnavigation-base"]="mobile-navigation-base"
moduleToStorageIdMap["libnavigation-core"]="mobile-navigation-core"
moduleToStorageIdMap["libnavigation-metrics"]="mobile-navigation-metrics"
moduleToStorageIdMap["libnavigator"]="mobile-navigation-navigator"
moduleToStorageIdMap["libtrip-notification"]="mobile-navigation-notification"
moduleToStorageIdMap["libdirections-hybrid"]="mobile-navigation-router"
moduleToStorageIdMap["libdirections-offboard"]="mobile-navigation-router-offboard"
moduleToStorageIdMap["libdirections-onboard"]="mobile-navigation-router-onboard"
moduleToStorageIdMap["libnavigation-util"]="mobile-navigation-utils"
moduleToStorageIdMap["libnavigation-ui"]="mobile-navigation-ui-v1"

function check_file_exists() {
  if [ ! -f $1 ]; then
    echo "$1 does not exist."
    exit 1
  fi
}

function get_s3_storage_path() {
  repo_type="releases"
  if [ $1 == true ]; then
    repo_type="snapshots"
  fi
  echo "s3://mapbox-api-downloads-production/v2/${moduleToStorageIdMap[$2]}/$repo_type/android/$3/maven/$4"
}

module="$1"
artifact_id="$2"
version="$3"
variant="$4"
if [ -z "$module" ]; then
  echo >&2 "First argument (module name) is missing."
  exit 1
fi
if [ -z "$artifact_id" ]; then
  echo >&2 "Second argument (Maven artifact ID) is missing."
  exit 1
fi
if [ -z "$version" ]; then
  echo >&2 "Third argument (version name) is missing."
  exit 1
fi
if [ -z "$variant" ]; then
  echo >&2 "Forth argument (variant, \"release\" or \"debug\") is missing."
  exit 1
fi

isSnapshot=false
if [[ "$version" == *-SNAPSHOT ]]; then
  isSnapshot=true
fi

pom_file_path="$module/build/publications/$variant/pom-default.xml"
check_file_exists $pom_file_path
pom_resulting_file_name="$artifact_id-$version.pom"

aar_file_path="$module/build/outputs/aar/$module-$variant.aar"
check_file_exists $aar_file_path
aar_resulting_file_name="$artifact_id-$version.aar"

javadoc_jar_file_path="$module/build/libs/$module-javadoc.jar"
check_file_exists $javadoc_jar_file_path
javadoc_jar_resulting_file_name="$artifact_id-$version-javadoc.jar"

sources_jar_file_path="$module/build/libs/$module-sources.jar"
check_file_exists $sources_jar_file_path
sources_jar_resulting_file_name="$artifact_id-$version-sources.jar"

aws s3 cp $pom_file_path $(get_s3_storage_path $isSnapshot $module $version $pom_resulting_file_name)
aws s3 cp $aar_file_path $(get_s3_storage_path $isSnapshot $module $version $aar_resulting_file_name)
aws s3 cp $javadoc_jar_file_path $(get_s3_storage_path $isSnapshot $module $version $javadoc_jar_resulting_file_name)
aws s3 cp $sources_jar_file_path $(get_s3_storage_path $isSnapshot $module $version $sources_jar_resulting_file_name)
