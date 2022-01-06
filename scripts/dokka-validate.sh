#!/bin/bash -e

EXIT_TYPE="exit 1"
MODULE=""
GRADLE_TASK="dokkaHtmlMultiModule"

SEARCH_REGEX='Undocumented: com.mapbox.navigation.'
SEARCH_REGEX_EXCEPT='\.Companion/ ' # skip `Companion object` headers

while getopts 'sm:' c
do
  case $c in
    s) EXIT_TYPE="" ;; # soft failing (optional)
    m) MODULE="${OPTARG}" && GRADLE_TASK="${MODULE}:dokkaHtml";; # module to validate dokka (optional)
  esac
done

echo "Dokka validation is starting"
echo "Validating kdoc ${MODULE} ..."
./gradlew $GRADLE_TASK | grep -i "$SEARCH_REGEX" | egrep -v "$SEARCH_REGEX_EXCEPT" && { echo 'kdoc validation failed'; ${EXIT_TYPE}; }
echo "Dokka validation has finished"
exit 0