MODULES = \
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
	$(call run-gradle-tasks,$(MODULES),ktlint) \
	&& $(call run-gradle-tasks,$(APPLICATION_MODULES),ktlint)

.PHONY: assemble-debug
assemble-debug:
	$(call run-gradle-tasks,$(MODULES),assembleDebug)

.PHONY: assemble-release
assemble-release:
	$(call run-gradle-tasks,$(MODULES),assembleRelease)

.PHONY: unit-tests
unit-tests:
	$(call run-gradle-tasks,$(MODULES),test)

.PHONY: unit-tests-jacoco
unit-tests-jacoco:
	$(call run-gradle-tasks,$(MODULES),jacocoTestReport)
