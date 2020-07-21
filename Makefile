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

.PHONY: check
check:
	$(call run-gradle-tasks,$(CORE_MODULES),ktlint) \
	&& $(call run-gradle-tasks,$(CORE_MODULES),checkstyle)

.PHONY: license-verification
license-verification:
	python ./scripts/validate-license.py

.PHONY: license
license:
	./gradlew licenseReleaseReport
	python ./scripts/generate-license.py

.PHONY: javadoc-dokka
javadoc-dokka:
	$(call run-gradle-tasks,$(CORE_MODULES),dokka)
	$(call run-gradle-tasks,$(UI_MODULES),dokka)

.PHONY: dependency-graphs
dependency-graphs:
	$(call run-gradle-tasks,$(CORE_MODULES),generateDependencyGraphMapboxLibraries) \
	&& $(call run-gradle-tasks,$(UI_MODULES),generateDependencyGraphMapboxLibraries)

.PHONY: dependency-updates
dependency-updates:
	$(call run-gradle-tasks,$(CORE_MODULES),dependencyUpdates) \
	&& $(call run-gradle-tasks,$(UI_MODULES),dependencyUpdates)

.PHONY: dex-count
dex-count:
	./gradlew countDebugDexMethods
	./gradlew countReleaseDexMethods

.PHONY: core-publish-local
core-publish-local:
	$(call run-gradle-tasks,$(CORE_MODULES),publishToMavenLocal)

.PHONY: ui-publish-local
ui-publish-local:
	$(call run-gradle-tasks,$(UI_MODULES),publishToMavenLocal)

.PHONY: build-core-debug
build-core-debug:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleDebug)

.PHONY: build-core-release
build-core-release:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleRelease)

.PHONY: core-unit-tests
core-unit-tests:
	$(call run-gradle-tasks,$(CORE_MODULES),test)

.PHONY: core-publish-to-sdk-registry
core-publish-to-sdk-registry:
	$(call run-gradle-tasks,$(CORE_MODULES),mapboxSDKRegistryUpload)

.PHONY: core-dependency-graph
core-dependency-graph:
	$(call run-gradle-tasks,$(CORE_MODULES),generateDependencyGraphMapboxLibraries)

.PHONY: core-check-api
core-check-api:
	./gradlew :libdirections-offboard:checkApi -PhidePackage=com.mapbox.navigation.route.offboard.internal
	./gradlew :libdirections-hybrid:checkApi -PhidePackage=com.mapbox.navigation.route.hybrid.internal
	./gradlew :libdirections-onboard:checkApi -PhidePackage=com.mapbox.navigation.route.onboard.internal
	./gradlew :libnavigation-base:checkApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:checkApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:checkApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:checkApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:checkApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :libnavigation-core:checkApi -PhidePackage=com.mapbox.navigation.core.internal

.PHONY: core-update-api
core-update-api:
	./gradlew :libdirections-offboard:updateApi -PhidePackage=com.mapbox.navigation.route.offboard.internal
	./gradlew :libdirections-hybrid:updateApi -PhidePackage=com.mapbox.navigation.route.hybrid.internal
	./gradlew :libdirections-onboard:updateApi -PhidePackage=com.mapbox.navigation.route.onboard.internal
	./gradlew :libnavigation-base:updateApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:updateApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:updateApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:updateApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:updateApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :libnavigation-core:updateApi -PhidePackage=com.mapbox.navigation.core.internal

.PHONY: build-ui-debug
build-ui-debug:
	$(call run-gradle-tasks,$(UI_MODULES),assembleDebug)

.PHONY: build-ui-release
build-ui-release:
	$(call run-gradle-tasks,$(UI_MODULES),assembleRelease)

.PHONY: ui-unit-tests
ui-unit-tests:
	$(call run-gradle-tasks,$(UI_MODULES),test)

.PHONY: ui-publish-to-sdk-registry
ui-publish-to-sdk-registry:
	$(call run-gradle-tasks,$(UI_MODULES),mapboxSDKRegistryUpload)

.PHONY: ui-check-api
ui-check-api:
	# TODO Remove -PhideId=ReferencesHidden after fixing errors
	./gradlew :libnavigation-ui:checkApi -PhidePackage=com.mapbox.navigation.ui.internal -PhideId=ReferencesHidden

.PHONY: ui-update-api
ui-update-api:
	./gradlew :libnavigation-ui:updateApi -PhidePackage=com.mapbox.navigation.ui.internal

.PHONY: update-metalava
update-metalava:
	sh ./scripts/update_metalava.sh