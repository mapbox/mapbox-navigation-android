CORE_MODULES = \
libnavigation-router \
libnavigation-base \
libnavigation-metrics \
libnavigation-util \
libnavigator \
libtrip-notification \
libnavigation-core \

UI_MODULES = \
libnavui-maps \
libnavui-base \
libnavui-util \

APPLICATION_MODULES = \
carbon \

define run-gradle-tasks
	for module in $(1) ; do \
		./gradlew $$module:$(2) || exit 1 ; \
	done
endef

.PHONY: check
check:
	$(call run-gradle-tasks,$(CORE_MODULES),ktlint) \
	&& $(call run-gradle-tasks,$(UI_MODULES),ktlint) \
	&& $(call run-gradle-tasks,$(APPLICATION_MODULES),ktlint)

.PHONY: javadoc-dokka
javadoc-dokka:
	$(call run-gradle-tasks,$(CORE_MODULES),dokka)
	$(call run-gradle-tasks,$(UI_MODULES),dokka)

.PHONY: assemble-debug
assemble-debug:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleDebug) \
	&& $(call run-gradle-tasks,$(UI_MODULES),assembleDebug)

.PHONY: assemble-release
assemble-release:
	$(call run-gradle-tasks,$(CORE_MODULES),assembleRelease) \
	&& $(call run-gradle-tasks,$(UI_MODULES),assembleRelease)

.PHONY: unit-tests
unit-tests:
	$(call run-gradle-tasks,$(CORE_MODULES),test) \
	&& $(call run-gradle-tasks,$(UI_MODULES),test)

.PHONY: unit-tests-jacoco
unit-tests-jacoco:
	$(call run-gradle-tasks,$(CORE_MODULES),jacocoTestReport) \
	&& $(call run-gradle-tasks,$(UI_MODULES),jacocoTestReport)

.PHONY: publish-local
publish-local:
	$(call run-gradle-tasks,$(CORE_MODULES),publishToMavenLocal) \
	&& $(call run-gradle-tasks,$(UI_MODULES),publishToMavenLocal)

.PHONY: upload-to-sdk-registry
upload-to-sdk-registry:
	$(call run-gradle-tasks,$(CORE_MODULES),mapboxSDKRegistryUpload) \
	&& $(call run-gradle-tasks,$(UI_MODULES),mapboxSDKRegistryUpload)

.PHONY: publish-to-sdk-registry
publish-to-sdk-registry:
	python3 -m pip install git-pull-request
	./gradlew mapboxSDKRegistryPublishAll
