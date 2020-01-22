checkstyle:
	./gradlew checkstyle

nitpick:
	./gradlew nitpick

license:
	./gradlew licenseReleaseReport
	python ./scripts/generate-license.py

test:
	# See libandroid-navigation/build.gradle for details
	./gradlew :libandroid-navigation:test
	./gradlew :libandroid-navigation-ui:test

build-release:
	./gradlew :libandroid-navigation:assembleRelease
	./gradlew :libandroid-navigation-ui:assembleRelease

javadoc:
	./gradlew :libandroid-navigation:javadocrelease
	./gradlew :libandroid-navigation-ui:javadocrelease

publish:
	export IS_LOCAL_DEVELOPMENT=false; ./gradlew :libandroid-navigation:uploadArchives
	export IS_LOCAL_DEVELOPMENT=false; ./gradlew :libandroid-navigation-ui:uploadArchives

publish-local:
	# This publishes to ~/.m2/repository/com/mapbox/mapboxsdk
	export IS_LOCAL_DEVELOPMENT=true; ./gradlew :libandroid-navigation:uploadArchives
	export IS_LOCAL_DEVELOPMENT=true; ./gradlew :libandroid-navigation-ui:uploadArchives

graphs:
	./gradlew :libandroid-navigation:generateDependencyGraphMapboxLibraries
	./gradlew :libandroid-navigation-ui:generateDependencyGraphMapboxLibraries

dependency-updates:
	./gradlew :libandroid-navigation:dependencyUpdates
	./gradlew :libandroid-navigation-ui:dependencyUpdates
	./gradlew :app:dependencyUpdates

dex-count:
	./gradlew countDebugDexMethods
	./gradlew countReleaseDexMethods

navigation-fixtures:
	# Navigation: Taylor street to Page street
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-122.413165,37.795042;-122.433378,37.7727?geometries=polyline6&overview=full&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o libandroid-navigation/src/test/resources/navigation.json

	# Directions: polyline geometry with precision 5
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-122.416667,37.783333;-121.900000,37.333333?geometries=polyline&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o libandroid-navigation/src/test/resources/directions_v5.json

	# Intersection:
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-101.70939001157072,33.62145406099651;-101.68721910152767,33.6213852194939?geometries=polyline6&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o libandroid-navigation/src/test/resources/single_intersection.json

	# Distance Congestion annotation: Mapbox DC to National Mall
	curl "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/-77.034042,38.899949;-77.03949,38.888871?geometries=polyline6&overview=full&steps=true&annotations=congestion%2Cdistance&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o libandroid-navigation/src/test/resources/directions_distance_congestion_annotation.json

	# Default Directions
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-122.416686,37.783425;-121.90034,37.333317?geometries=polyline6&steps=true&banner_instructions=true&voice_instructions=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o libandroid-navigation/src/test/resources/directions_v5_precision_6.json

    # No VoiceInstructions
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-77.034013,38.899994;-77.033757,38.903311?geometries=polyline6&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o libandroid-navigation/src/test/resources/directions_v5_no_voice.json

.PHONY: 1.0-build-debug
1.0-build-debug:
	./gradlew :libdirections-offboard:assembleDebug
	./gradlew :libdirections-hybrid:assembleDebug
	./gradlew :libdirections-onboard:assembleDebug
	./gradlew :libnavigation-base:assembleDebug
	./gradlew :libnavigation-metrics:assembleDebug
	./gradlew :libnavigation-util:assembleDebug
	./gradlew :libnavigator:assembleDebug
	./gradlew :libtrip-notification:assembleDebug
	./gradlew :libnavigation-core:assembleDebug

.PHONY: 1.0-build-release
1.0-build-release:
	./gradlew :libdirections-offboard:assembleRelease
	./gradlew :libdirections-hybrid:assembleRelease
	./gradlew :libdirections-onboard:assembleRelease
	./gradlew :libnavigation-base:assembleRelease
	./gradlew :libnavigation-metrics:assembleRelease
	./gradlew :libnavigation-util:assembleRelease
	./gradlew :libnavigator:assembleRelease
	./gradlew :libtrip-notification:assembleRelease
	./gradlew :libnavigation-core:assembleRelease

.PHONY: 1.0-unit-tests
1.0-unit-tests:
	./gradlew :libdirections-hybrid:test
	./gradlew :libdirections-offboard:test
	./gradlew :libdirections-onboard:test
	./gradlew :liblogger:test
	./gradlew :libnavigation-base:test
	./gradlew :libnavigation-metrics:test
	./gradlew :libnavigation-util:test
	./gradlew :libnavigator:test
	./gradlew :libtrip-notification:test
	./gradlew :libnavigation-core:test

.PHONY: 1.0-publish-to-bintray
1.0-publish-to-bintray:
	./gradlew :libdirections-offboard:bintrayUpload
	./gradlew :libdirections-hybrid:bintrayUpload
	./gradlew :libdirections-onboard:bintrayUpload
	./gradlew :libnavigation-base:bintrayUpload
	./gradlew :libnavigation-metrics:bintrayUpload
	./gradlew :libnavigation-util:bintrayUpload
	./gradlew :libnavigator:bintrayUpload
	./gradlew :libtrip-notification:bintrayUpload
	./gradlew :libnavigation-core:bintrayUpload

.PHONY: 1.0-publish-to-artifactory
1.0-publish-to-artifactory:
	./gradlew :libdirections-offboard:artifactoryPublish
	./gradlew :libdirections-hybrid:artifactoryPublish
	./gradlew :libdirections-onboard:artifactoryPublish
	./gradlew :libnavigation-base:artifactoryPublish
	./gradlew :libnavigation-metrics:artifactoryPublish
	./gradlew :libnavigation-util:artifactoryPublish
	./gradlew :libnavigator:artifactoryPublish
	./gradlew :libtrip-notification:artifactoryPublish
	./gradlew :libnavigation-core:artifactoryPublish