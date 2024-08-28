RELEASED_CORE_MODULES = \
libnavigation-router \
libnavigation-base \
libnavigation-metrics \
libnavigation-util \
libnavigator \
libtrip-notification \
libnavigation-core \
libnavigation-copilot \

CORE_MODULES = $(RELEASED_CORE_MODULES)

RELEASED_UI_MODULES = \
libnavui-maps \
libnavui-base \
libnavui-util \
libnavui-tripprogress \
libnavui-maneuver \
libnavui-resources \
libnavui-voice \
libnavigation-android \
libnavui-speedlimit \
libnavui-shield \
libnavui-status \
libnavui-dropin \
libnavui-app \

UI_MODULES = $(RELEASED_UI_MODULES)

ANDROIDAUTO_MODULES = \
libnavui-androidauto

APPLICATION_MODULES = \
qa-test-app \
examples \
instrumentation-tests \
android-auto-app \

define run-gradle-tasks
    COMMAND="./gradlew"; \
	for module in $(1); do \
	    COMMAND="$${COMMAND} $$module:$(2)"; \
	done; \
	echo "executing $$COMMAND"; \
	eval $$COMMAND
endef

.PHONY: check-kotlin-lint
check-kotlin-lint:
	$(call run-gradle-tasks,$(CORE_MODULES),ktlint) \
	&& $(call run-gradle-tasks,$(UI_MODULES),ktlint) \
	&& $(call run-gradle-tasks,$(ANDROIDAUTO_MODULES),ktlint) \
	&& $(call run-gradle-tasks,$(APPLICATION_MODULES),ktlint)

.PHONY: format-kotlin-lint
format-kotlin-lint:
	$(call run-gradle-tasks,$(CORE_MODULES),ktlintFormat) \
	&& $(call run-gradle-tasks,$(UI_MODULES),ktlintFormat) \
	&& $(call run-gradle-tasks,$(ANDROIDAUTO_MODULES),ktlintFormat) \
	&& $(call run-gradle-tasks,$(APPLICATION_MODULES),ktlintFormat)

.PHONY: check-android-lint
check-android-lint:
	$(call run-gradle-tasks,$(CORE_MODULES),lint) \
	&& $(call run-gradle-tasks,$(UI_MODULES),lint) \
	&& $(call run-gradle-tasks,$(ANDROIDAUTO_MODULES),lint) \
	&& $(call run-gradle-tasks,$(APPLICATION_MODULES),lint)

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

.PHONY: javadoc-dokka-androidauto
javadoc-dokka-androidauto:
	./gradlew libnavui-androidauto:dokkaHtml

.PHONY: dependency-graphs
dependency-graphs:
	$(call run-gradle-tasks,$(CORE_MODULES),generateDependencyGraphMapboxLibraries) \
	&& $(call run-gradle-tasks,$(UI_MODULES),generateDependencyGraphMapboxLibraries) \

.PHONY: dependency-updates
dependency-updates:
	$(call run-gradle-tasks,$(CORE_MODULES),dependencyUpdates) \
	&& $(call run-gradle-tasks,$(UI_MODULES),dependencyUpdates) \

.PHONY: verify-common-sdk-version
verify-common-sdk-version:
	./gradlew verifyCommonSdkVersion

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
core-unit-tests-release-jacoco:
	$(call run-gradle-tasks,$(CORE_MODULES),jacocoTestReleaseUnitTestReport)

.PHONY: core-dependency-graph
core-dependency-graph:
	$(call run-gradle-tasks,$(CORE_MODULES),generateDependencyGraphMapboxLibraries)

.PHONY: core-check-api
core-check-api: assemble-core-release
	./gradlew :libnavigation-router:checkApi -PhidePackage=com.mapbox.navigation.route.internal
	./gradlew :libnavigation-base:checkApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:checkApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:checkApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:checkApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:checkApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :libnavigation-core:checkApi -PhidePackage=com.mapbox.navigation.core.internal
	./gradlew :libnavigation-copilot:checkApi -PhidePackage=com.mapbox.navigation.copilot.internal

.PHONY: core-update-api
core-update-api: assemble-core-release
	./gradlew :libnavigation-router:updateApi -PhidePackage=com.mapbox.navigation.route.internal
	./gradlew :libnavigation-base:updateApi -PhidePackage=com.mapbox.navigation.base.internal
	./gradlew :libnavigation-metrics:updateApi -PhidePackage=com.mapbox.navigation.metrics.internal
	./gradlew :libnavigation-util:updateApi -PhidePackage=com.mapbox.navigation.utils.internal
	./gradlew :libnavigator:updateApi -PhidePackage=com.mapbox.navigation.navigator.internal
	./gradlew :libtrip-notification:updateApi -PhidePackage=com.mapbox.navigation.trip.notification.internal
	./gradlew :libnavigation-core:updateApi -PhidePackage=com.mapbox.navigation.core.internal
	./gradlew :libnavigation-copilot:updateApi -PhidePackage=com.mapbox.navigation.copilot.internal

.PHONY: assemble-ui-debug
assemble-ui-debug:
	$(call run-gradle-tasks,$(UI_MODULES),assembleDebug)

.PHONY: assemble-ui-release
assemble-ui-release:
	$(call run-gradle-tasks,$(UI_MODULES),assembleRelease $(additional_gradle_parameters))

.PHONY: assemble-androidauto-release
assemble-androidauto-release:
	$(call run-gradle-tasks,$(ANDROIDAUTO_MODULES),assembleRelease)

.PHONY: ui-unit-tests
ui-unit-tests:
	$(call run-gradle-tasks,$(UI_MODULES),test)

.PHONY: ui-unit-tests-jacoco
ui-unit-tests-jacoco:
	$(call run-gradle-tasks,$(UI_MODULES),jacocoTestReport)

.PHONY: ui-unit-tests-release-jacoco
ui-unit-tests-release-jacoco:
	$(call run-gradle-tasks,$(UI_MODULES),jacocoTestReleaseUnitTestReport)

.PHONY: publish-local
publish-local:
	./gradlew publishToMavenLocal

.PHONY: upload-to-sdk-registry-snapshot
upload-to-sdk-registry-snapshot:
	./gradlew mapboxSDKRegistryUpload -Psnapshot=true;

.PHONY: upload-to-sdk-registry
upload-to-sdk-registry:
	./gradlew mapboxSDKRegistryUpload -x libnavui-androidauto:mapboxSDKRegistryUpload;

.PHONY: publish-to-sdk-registry
publish-to-sdk-registry:
	@if [ -z "$(GITHUB_TOKEN)" ]; then \
		echo "GITHUB_TOKEN env variable has to be set"; \
	else \
		python3 -m pip install git-pull-request; \
		./gradlew mapboxSDKRegistryPublishAll; \
	fi

.PHONY: upload-to-sdk-registry-androidauto
upload-to-sdk-registry-androidauto:
	./gradlew libnavui-androidauto:mapboxSDKRegistryUpload;

.PHONY: publish-to-sdk-registry-androidauto
publish-to-sdk-registry-androidauto:
	@if [ -z "$(GITHUB_TOKEN)" ]; then \
		echo "GITHUB_TOKEN env variable has to be set"; \
	else \
		python3 -m pip install git-pull-request; \
		./gradlew libnavui-androidauto:mapboxSDKRegistryPublish; \
	fi

.PHONY: ui-check-api
ui-check-api: assemble-ui-release
	# TODO Remove -PhideId=ReferencesHidden after fixing errors
	./gradlew :libnavui-maps:checkApi -PhidePackage=com.mapbox.navigation.ui.maps.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-base:checkApi -PhidePackage=com.mapbox.navigation.ui.base.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-util:checkApi -PhidePackage=com.mapbox.navigation.ui.utils.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-maneuver:checkApi -PhidePackage=com.mapbox.navigation.ui.maneuver.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-tripprogress:checkApi -PhidePackage=com.mapbox.navigation.ui.tripprogress.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-voice:checkApi -PhidePackage=com.mapbox.navigation.ui.voice.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-shield:checkApi -PhidePackage=com.mapbox.navigation.ui.shield.internal
	./gradlew :libnavui-speedlimit:checkApi -PhidePackage=com.mapbox.navigation.ui.speedlimit.internal -PhideId=ReferencesHidden
	./gradlew :libnavui-status:checkApi -PhidePackage=com.mapbox.navigation.ui.status.internal
	./gradlew :libnavui-dropin:checkApi -PhidePackage=com.mapbox.navigation.dropin.internal
	./gradlew :libnavui-app:checkApi -PhidePackage=com.mapbox.navigation.ui.app.internal

.PHONY: ui-update-api
ui-update-api: assemble-ui-release
	./gradlew :libnavui-maps:updateApi -PhidePackage=com.mapbox.navigation.ui.maps.internal
	./gradlew :libnavui-base:updateApi -PhidePackage=com.mapbox.navigation.ui.base.internal
	./gradlew :libnavui-util:updateApi -PhidePackage=com.mapbox.navigation.ui.utils.internal
	./gradlew :libnavui-maneuver:updateApi -PhidePackage=com.mapbox.navigation.ui.maneuver.internal
	./gradlew :libnavui-tripprogress:updateApi -PhidePackage=com.mapbox.navigation.ui.tripprogress.internal
	./gradlew :libnavui-voice:updateApi -PhidePackage=com.mapbox.navigation.ui.voice.internal
	./gradlew :libnavui-shield:updateApi -PhidePackage=com.mapbox.navigation.ui.shield.internal
	./gradlew :libnavui-speedlimit:updateApi -PhidePackage=com.mapbox.navigation.ui.speedlimit.internal
	./gradlew :libnavui-status:updateApi -PhidePackage=com.mapbox.navigation.ui.status.internal
	./gradlew :libnavui-dropin:updateApi -PhidePackage=com.mapbox.navigation.dropin.internal
	./gradlew :libnavui-app:updateApi -PhidePackage=com.mapbox.navigation.ui.app.internal

.PHONY: androidauto-check-api
androidauto-check-api: assemble-androidauto-release
	./gradlew :libnavui-androidauto:checkApi -PhidePackage=com.mapbox.androidauto.internal

.PHONY: androidauto-update-api
androidauto-update-api: assemble-androidauto-release
	./gradlew :libnavui-androidauto:updateApi -PhidePackage=com.mapbox.androidauto.internal

.PHONY: update-metalava
update-metalava:
	sh ./scripts/update_metalava.sh

# Android Auto helper command. Set up your environment to run the desktop car emulator.
# Guidance available in the android-auto README: /libnavui-androidauto/README.md
.PHONY: car
car:
	adb forward tcp:5277 tcp:5277
	cd $(ANDROID_HOME)/extras/google/auto/ && ./desktop-head-unit
