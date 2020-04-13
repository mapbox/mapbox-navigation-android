#!/bin/bash -e

EXIT_TYPE="exit 1"
MODULE=""

SEARCH_REGEX='No documentation for com.mapbox.navigation'
SEARCH_REGEX_EXCEPT='\.Companion ' # skip `Companion object` headers

while getopts 'sm:' c
do
  case $c in
    s) EXIT_TYPE="" ;; # soft failing (optional)
    m) MODULE="${OPTARG}:";; # module to validate dokka (optional)
  esac
done


echo "Dokka validation is starting; clean project"
./gradlew clean
echo "Dokka is validating ${MODULE}"
./gradlew "${MODULE}dokka" | grep -i "$SEARCH_REGEX" | grep -v "$SEARCH_REGEX_EXCEPT" && { echo 'kdoc validation failed'; ${EXIT_TYPE}; }
echo "Dokka validation has finished"
exit 0