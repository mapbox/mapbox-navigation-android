PUBLIC_API_PREFIX = public-api

# $(1) modules' names(e.g. "navigation, base...")
define reports-copy-to
	for module in $(1) ; do \
		if [ -d "$${module}/build/jacoco" ]; then \
			cp $${module}/build/jacoco/jacoco.xml $(2)/$${module}.xml || exit 1 & \
		else \
			echo "Directory $${module}/build/jacoco does not exist. Skipping." ; \
		fi \
	done
endef

RELEASED_CORE_MODULES = \
base \
libnavigation-metrics \
libnavigation-util \
libnavigator \
libtrip-notification \
navigation \
copilot \
tripdata \
voice \
libtesting-router

CORE_MODULES = $(RELEASED_CORE_MODULES)

RELEASED_UI_MODULES = \
ui-maps \
ui-components \
androidauto \
libnavui-base \
libnavui-util \
libnavigation-android

UI_MODULES = $(RELEASED_UI_MODULES)

PUBLIC_API_MODULES = $(CORE_MODULES) $(UI_MODULES)

APPLICATION_MODULES = \
instrumentation-tests

define run-gradle-tasks
	COMMAND="./gradlew"; \
	for module in $(1); do \
		COMMAND="$${COMMAND} $$module:$(2)"; \
	done; \
	echo "executing $$COMMAND"; \
	eval $$COMMAND;
endef

.PHONY: check-kotlin-lint
check-kotlin-lint:
	$(call run-gradle-tasks,$(CORE_MODULES),ktlint)
	$(call run-gradle-tasks,$(UI_MODULES),ktlint)
	$(call run-gradle-tasks,$(APPLICATION_MODULES),ktlint)

.PHONY: format-kotlin-lint
format-kotlin-lint:
	$(call run-gradle-tasks,$(CORE_MODULES),ktlintFormat)
	$(call run-gradle-tasks,$(UI_MODULES),ktlintFormat)
	$(call run-gradle-tasks,$(APPLICATION_MODULES),ktlintFormat)

.PHONY: check-android-lint
check-android-lint:
	$(call run-gradle-tasks,$(CORE_MODULES),lint)
	$(call run-gradle-tasks,$(UI_MODULES),lint)
	$(call run-gradle-tasks,$(APPLICATION_MODULES),lint)

.PHONY: license-verification
license-verification:
	python ./scripts/validate-license.py

.PHONY: license
license:
	./gradlew licenseReleaseReport
	python3 ./scripts/generate-license.py

.PHONY: javadoc-dokka
javadoc-dokka:
	./gradlew dokkaHtmlMultiModule
	./docs/replace-styles.sh

.PHONY: dependency-graphs
dependency-graphs:
	$(call run-gradle-tasks,$(CORE_MODULES),generateDependencyGraphMapboxLibraries)
	$(call run-gradle-tasks,$(UI_MODULES),generateDependencyGraphMapboxLibraries)

.PHONY: dependency-updates
dependency-updates:
	$(call run-gradle-tasks,$(CORE_MODULES),dependencyUpdates)
	$(call run-gradle-tasks,$(UI_MODULES),dependencyUpdates)

.PHONY: find-all-common-sdk-versions
find-all-common-sdk-versions:
	./gradlew findAllCommonSdkVersions

.PHONY: dex-count
dex-count:
	./gradlew countDebugDexMethods
	./gradlew countReleaseDexMethods

.PHONY: assemble-core-debug
assemble-core-debug:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleDebug)

.PHONY: assemble-core-release
assemble-core-release:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleRelease $(additional_gradle_parameters))

.PHONY: core-unit-tests
core-unit-tests:
	$(call run-gradle-tasks,$(CORE_MODULES),test)

.PHONY: core-unit-tests-jacoco
core-unit-tests-jacoco:
	$(call run-gradle-tasks,$(CORE_MODULES),jacocoTestReport)

.PHONY: core-unit-tests-release-jacoco
.SILENT: core-unit-tests-release-jacoco
core-unit-tests-release-jacoco:
	$(call run-gradle-tasks,$(CORE_MODULES),jacocoTestReleaseUnitTestReport)

# Prepare core coverage
.PHONY: prepare-core-coverage-reports
.SILENT: prepare-core-coverage-reports
prepare-core-coverage-reports: core-unit-tests-release-jacoco
	rm -rf $(CORE_REPORTS_DIR) \
	&& mkdir -p $(CORE_REPORTS_DIR) \
	&& $(call reports-copy-to,$(RELEASED_CORE_MODULES),$(CORE_REPORTS_DIR)) \
	&& wait

.PHONY: core-dependency-graph
core-dependency-graph:
	$(call run-gradle-tasks,$(CORE_MODULES),generateDependencyGraphMapboxLibraries)

.PHONY: core-check-api
core-check-api: assemble-core-release
	./gradlew :base:checkApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:checkApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:checkApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:checkApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:checkApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :navigation:checkApi -PhidePackage=com.mapbox.navigation.core.internal
	./gradlew :copilot:checkApi -PhidePackage=com.mapbox.navigation.copilot.internal
	./gradlew :tripdata:checkApi -PhidePackage=com.mapbox.navigation.tripdata.internal,com.mapbox.navigation.tripdata.maneuver.internal,com.mapbox.navigation.tripdata.progress.internal,com.mapbox.navigation.tripdata.shield.internal,com.mapbox.navigation.tripdata.speedlimit.internal
	./gradlew :voice:checkApi -PhidePackage=com.mapbox.navigation.voice.internal -PhideId=ReferencesHidden
	./gradlew :libtesting-router:checkApi -PhidePackage=com.mapbox.navigation.testing.router.internal

.PHONY: core-update-api
core-update-api: assemble-core-release
	./gradlew :base:updateApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:updateApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:updateApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:updateApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:updateApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :navigation:updateApi -PhidePackage=com.mapbox.navigation.core.internal
	./gradlew :copilot:updateApi -PhidePackage=com.mapbox.navigation.copilot.internal
	./gradlew :tripdata:updateApi -PhidePackage=com.mapbox.navigation.tripdata.internal,com.mapbox.navigation.tripdata.maneuver.internal,com.mapbox.navigation.tripdata.progress.internal,com.mapbox.navigation.tripdata.shield.internal,com.mapbox.navigation.tripdata.speedlimit.internal
	./gradlew :voice:updateApi -PhidePackage=com.mapbox.navigation.voice.internal
	./gradlew :libtesting-router:updateApi -PhidePackage=com.mapbox.navigation.testing.router.internal

.PHONY: assemble-ui-debug
assemble-ui-debug:
	$(call run-gradle-tasks,$(UI_MODULES),assembleDebug)

.PHONY: assemble-ui-release
assemble-ui-release:
	$(call run-gradle-tasks,$(UI_MODULES),assembleRelease $(additional_gradle_parameters))

.PHONY: ui-unit-tests
ui-unit-tests:
	$(call run-gradle-tasks,$(UI_MODULES),test)

.PHONY: ui-unit-tests-jacoco
ui-unit-tests-jacoco:
	$(call run-gradle-tasks,$(UI_MODULES),jacocoTestReport)

.PHONY: ui-unit-tests-release-jacoco
.SILENT: core-unit-tests-release-jacoco
ui-unit-tests-release-jacoco:
	$(call run-gradle-tasks,$(UI_MODULES),jacocoTestReleaseUnitTestReport)

# Prepare ui coverage
.PHONY: prepare-ui-coverage-reports
.SILENT: prepare-ui-coverage-reports
prepare-ui-coverage-reports: ui-unit-tests-release-jacoco
	rm -rf $(UI_REPORTS_DIR) \
    && mkdir -p $(UI_REPORTS_DIR) \
	&& $(call reports-copy-to,$(RELEASED_UI_MODULES),$(UI_REPORTS_DIR)) \
	&& wait

.PHONY: publish-local
publish-local:
	./gradlew publishToMavenLocal

.PHONY: upload-to-sdk-registry-snapshot
upload-to-sdk-registry-snapshot:
	./gradlew mapboxSDKRegistryUpload -Psnapshot=true -PVERSION_NAME=$(VERSION_NAME);

.PHONY: upload-to-sdk-registry
upload-to-sdk-registry:
	./gradlew mapboxSDKRegistryUpload;

.PHONY: publish-to-sdk-registry
publish-to-sdk-registry:
	@if [ -z "$(GITHUB_TOKEN)" ]; then \
		echo "GITHUB_TOKEN env variable has to be set"; \
	else \
		python3 -m pip install git-pull-request; \
		./gradlew mapboxSDKRegistryPublishAll; \
	fi

.PHONY: ui-check-api
ui-check-api: assemble-ui-release
	# TODO Remove -PhideId=ReferencesHidden after fixing errors
	./gradlew :ui-maps:checkApi -PhidePackage=com.mapbox.navigation.ui.maps.internal -PhideId=ReferencesHidden
	./gradlew :ui-components:updateApi -PhidePackage=com.mapbox.navigation.ui.components.internal,com.mapbox.navigation.ui.components.maneuver.internal,com.mapbox.navigation.ui.components.maps.internal,com.mapbox.navigation.ui.components.speedlimit.internal,com.mapbox.navigation.ui.components.status.internal,com.mapbox.navigation.ui.components.tripprogress.internal,com.mapbox.navigation.ui.components.voice.internal
	./gradlew :androidauto:updateApi -PhidePackage=com.mapbox.navigation.ui.androidauto.internal
	./gradlew :libnavui-base:checkApi -PhidePackage=com.mapbox.navigation.ui.base.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-util:checkApi -PhidePackage=com.mapbox.navigation.ui.utils.internal -PhideId=ReferencesHidden

.PHONY: ui-update-api
ui-update-api: assemble-ui-release
	./gradlew :ui-maps:updateApi -PhidePackage=com.mapbox.navigation.ui.maps.internal
	./gradlew :ui-components:updateApi -PhidePackage=com.mapbox.navigation.ui.components.internal,com.mapbox.navigation.ui.components.maneuver.internal,com.mapbox.navigation.ui.components.maps.internal,com.mapbox.navigation.ui.components.speedlimit.internal,com.mapbox.navigation.ui.components.status.internal,com.mapbox.navigation.ui.components.tripprogress.internal,com.mapbox.navigation.ui.components.voice.internal
	./gradlew :androidauto:updateApi -PhidePackage=com.mapbox.navigation.ui.androidauto.internal
	./gradlew :libnavui-base:updateApi -PhidePackage=com.mapbox.navigation.ui.base.internal
	./gradlew :libnavui-util:updateApi -PhidePackage=com.mapbox.navigation.ui.utils.internal

.PHONY: update-metalava
update-metalava:
	sh ./scripts/update_metalava.sh

# Android Auto helper command. Set up your environment to run the desktop car emulator.
# Guidance available in the android-auto README: /libnavigation-extensions/androidauto/README.md
.PHONY: car
car:
	adb forward tcp:5277 tcp:5277
	cd $(ANDROID_HOME)/extras/google/auto/ && ./desktop-head-unit
