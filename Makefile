checkstyle:
	cd navigation; ./gradlew checkstyle

test:
	cd navigation; ./gradlew :libjava-core:test

build-release:
	cd navigation; ./gradlew :libandroid-telemetry:assembleRelease

javadoc:
	cd navigation; ./gradlew :libandroid-ui:javadocrelease

publish:
	cd navigation; export IS_LOCAL_DEVELOPMENT=false; ./gradlew :libandroid-ui:uploadArchives

publish-local:
	# This publishes to ~/.m2/repository/com/mapbox/mapboxsdk
	cd navigation; export IS_LOCAL_DEVELOPMENT=true; ./gradlew :libandroid-ui:uploadArchives

dex-count:
	cd navigation; ./gradlew countDebugDexMethods
	cd navigation; ./gradlew countReleaseDexMethods

navigation-fixtures:
	# Navigation: Taylor street to Page street
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-122.413165,37.795042;-122.433378,37.7727?geometries=polyline6&overview=full&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o mapbox/libandroid-services/src/test/resources/navigation.json

	# Directions: polyline geometry with precision 5
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-122.416667,37.783333;-121.900000,37.333333?geometries=polyline&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o mapbox/libjava-services/src/test/fixtures/directions_v5.json

libosrm:
	rm -rf mapbox/libjava-services/src/main/resources/translations mapbox/libjava-services/src/test/fixtures/osrm/v5
	mkdir -p mapbox/libjava-services/src/main/resources/translations mapbox/libjava-services/src/test/fixtures/osrm/v5
	cp -R ../osrm-text-instructions/languages/translations/* mapbox/libjava-services/src/main/resources/translations
	cp -R ../osrm-text-instructions/test/fixtures/v5/* mapbox/libjava-services/src/test/fixtures/osrm/v5
