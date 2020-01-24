#!/bin/bash -e
./gradlew clean
./gradlew :libnavigator:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :libnavigation-base:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :libdirections-onboard:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :libdirections-offboard:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :libdirections-hybrid:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :libnavigation-metrics:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :libtrip-notification:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :liblogger:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
./gradlew :libnavigation-core:dokka -i | grep -i 'No documentation for com' && { echo 'kdoc validation failed'; exit 1; }
exit 0