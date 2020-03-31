#!/bin/bash -e
echo "Dokka validation is starting; clean project"
./gradlew clean
echo "Dokka is validating :libnavigator"
./gradlew :libnavigator:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka is validating :libnavigation-base"
./gradlew :libnavigation-base:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka is validating :libdirections-onboard"
./gradlew :libdirections-onboard:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka is validating :libdirections-offboard"
./gradlew :libdirections-offboard:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka is validating :libdirections-hybrid"
./gradlew :libdirections-hybrid:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka is validating :libnavigation-metrics"
./gradlew :libnavigation-metrics:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka is validating :libtrip-notification"
./gradlew :libtrip-notification:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka is validating :libnavigation-core"
./gradlew :libnavigation-core:dokka -i | grep -i 'No documentation for com.mapbox.navigation' && { echo 'kdoc validation failed'; exit 1; }
echo "Dokka validation has finished"
exit 0