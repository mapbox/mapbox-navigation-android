CORE_MODULES = \
libdirections-offboard \
libdirections-hybrid \
libdirections-onboard \
libnavigation-base \
libnavigation-metrics \
libnavigation-util \
libnavigator \
libtrip-notification \
libnavigation-core \

UI_MODULES = \
libnavigation-ui \

define run-gradle-tasks
	for module in $(1) ; do \
		./gradlew $$module:$(2) ; \
	done
endef

checkstyle:
	./gradlew checkstyle

license-verification:
	python ./scripts/validate-license.py

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

javadoc-dokka:
	$(call run-gradle-tasks,$(CORE_MODULES),dokka)
	$(call run-gradle-tasks,$(UI_MODULES),dokka)

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

.PHONY: 1.0-core-publish-local
1.0-core-publish-local:
	$(call run-gradle-tasks,$(CORE_MODULES),publishToMavenLocal)

.PHONY: 1.0-ui-publish-local
1.0-ui-publish-local:
	$(call run-gradle-tasks,$(UI_MODULES),publishToMavenLocal)

.PHONY: 1.0-build-core-debug
1.0-build-core-debug:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleDebug)

.PHONY: 1.0-build-core-release
1.0-build-core-release:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleRelease)

.PHONY: 1.0-core-unit-tests
1.0-core-unit-tests:
	$(call run-gradle-tasks,$(CORE_MODULES),test)

.PHONY: 1.0-core-publish-to-bintray
1.0-core-publish-to-bintray:
	$(call run-gradle-tasks,$(CORE_MODULES),bintrayUpload)

.PHONY: 1.0-core-publish-to-artifactory
1.0-core-publish-to-artifactory:
	$(call run-gradle-tasks,$(CORE_MODULES),artifactoryPublish)

.PHONY: 1.0-core-publish-to-sdk-registry
1.0-core-publish-to-sdk-registry:
	$(call run-gradle-tasks,$(CORE_MODULES),sdkRegistryPublishRelease)

.PHONY: 1.0-core-dependency-graph
1.0-core-dependency-graph:
	$(call run-gradle-tasks,$(CORE_MODULES),generateDependencyGraphMapboxLibraries)

.PHONY: 1.0-core-check-api
1.0-core-check-api:
	./gradlew :libdirections-offboard:checkApi -PhidePackage=com.mapbox.navigation.route.offboard.internal
	./gradlew :libdirections-hybrid:checkApi -PhidePackage=com.mapbox.navigation.route.hybrid.internal
	./gradlew :libdirections-onboard:checkApi -PhidePackage=com.mapbox.navigation.route.onboard.internal
	./gradlew :libnavigation-base:checkApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:checkApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:checkApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:checkApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:checkApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :libnavigation-core:checkApi -PhidePackage=com.mapbox.navigation.core.internal

.PHONY: 1.0-core-update-api
1.0-core-update-api:
	./gradlew :libdirections-offboard:updateApi -PhidePackage=com.mapbox.navigation.route.offboard.internal
	./gradlew :libdirections-hybrid:updateApi -PhidePackage=com.mapbox.navigation.route.hybrid.internal
	./gradlew :libdirections-onboard:updateApi -PhidePackage=com.mapbox.navigation.route.onboard.internal
	./gradlew :libnavigation-base:updateApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:updateApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:updateApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:updateApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:updateApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :libnavigation-core:updateApi -PhidePackage=com.mapbox.navigation.core.internal

.PHONY: 1.0-build-ui-debug
1.0-build-ui-debug:
	$(call run-gradle-tasks,$(UI_MODULES),assembleDebug)

.PHONY: 1.0-build-ui-release
1.0-build-ui-release:
	$(call run-gradle-tasks,$(UI_MODULES),assembleRelease)

.PHONY: 1.0-ui-unit-tests
1.0-ui-unit-tests:
	$(call run-gradle-tasks,$(UI_MODULES),test)

.PHONY: 1.0-ui-publish-to-bintray
1.0-ui-publish-to-bintray:
	$(call run-gradle-tasks,$(UI_MODULES),bintrayUpload)

.PHONY: 1.0-ui-publish-to-artifactory
1.0-ui-publish-to-artifactory:
	$(call run-gradle-tasks,$(UI_MODULES),artifactoryPublish)

.PHONY: 1.0-ui-publish-to-sdk-registry
1.0-ui-publish-to-sdk-registry:
	$(call run-gradle-tasks,$(UI_MODULES),sdkRegistryPublishRelease)

.PHONY: 1.0-ui-check-api
1.0-ui-check-api:
	# TODO Remove -PhideId=ReferencesHidden after fixing errors
	./gradlew :libnavigation-ui:checkApi -PhidePackage=com.mapbox.navigation.ui.internal -PhideId=ReferencesHidden

.PHONY: 1.0-ui-update-api
1.0-ui-update-api:
	./gradlew :libnavigation-ui:updateApi -PhidePackage=com.mapbox.navigation.ui.internal

.PHONY: update-metalava
update-metalava:
	sh ./scripts/update_metalava.sh