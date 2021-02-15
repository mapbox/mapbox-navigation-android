#!/bin/bash -e

EXIT_TYPE="exit 1"
MODULE=""
GRADLE_TASK="dokkaHtmlMultiModule"

fileItemString=$(cat  scripts/undocumented_classes.txt |tr "\n" " ")
UNDOCUMENTED_CLASSES=($fileItemString)

echo "Array = ${UNDOCUMENTED_CLASSES[*]}"

SEARCH_REGEX='Undocumented: com.mapbox.navigation.'
SEARCH_REGEX_EXCEPT='\.Companion/ ' # skip `Companion object` headers

while getopts 'sm:' c
do
  case $c in
    s) EXIT_TYPE="" ;; # soft failing (optional)
    m) MODULE="${OPTARG}" && GRADLE_TASK="${MODULE}:dokkaHtml";; # module to validate dokka (optional)
  esac
done


FINAL_SEARCH_REGEX_EXCEPT="${SEARCH_REGEX_EXCEPT}"
for i in "${UNDOCUMENTED_CLASSES[@]}"; do
    FINAL_SEARCH_REGEX_EXCEPT="${FINAL_SEARCH_REGEX_EXCEPT}|${i}"
done

echo "Dokka validation is starting; clean project"
./gradlew clean
echo "Dokka is validating ${MODULE}"
./gradlew $GRADLE_TASK | grep -i "$SEARCH_REGEX" | egrep -v "$FINAL_SEARCH_REGEX_EXCEPT" && { echo 'kdoc validation failed'; ${EXIT_TYPE}; }
echo "Dokka validation has finished"
exit 0