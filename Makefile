OSRM_PATH_TRANSLATIONS = navigation/libandroid-navigation/src/main/res/raw/translations
OSRM_PATH_FIXTURES     = navigation/libandroid-navigation/src/test/res/osrm/v5

prepare-osrm:
	rm -rf $(OSRM_PATH_TRANSLATIONS) $(OSRM_PATH_FIXTURES)
	mkdir -p $(OSRM_PATH_TRANSLATIONS) $(OSRM_PATH_FIXTURES)
	cp -R ../osrm-text-instructions/languages/translations/* $(OSRM_PATH_TRANSLATIONS)
	cp -R ../osrm-text-instructions/test/fixtures/v5/* $(OSRM_PATH_FIXTURES)

checkstyle:
	cd navigation; ./gradlew checkstyle

test:
	# See navigation/libandroid-navigation/build.gradle for details
	cd navigation; ./gradlew :libandroid-navigation:test

build-release:
	cd navigation; ./gradlew :libandroid-navigation:assembleRelease

javadoc:
	cd navigation; ./gradlew :libandroid-navigation:javadocrelease

publish:
	cd navigation; export IS_LOCAL_DEVELOPMENT=false; ./gradlew :libandroid-navigation:uploadArchives

publish-local:
	# This publishes to ~/.m2/repository/com/mapbox/mapboxsdk
	cd navigation; export IS_LOCAL_DEVELOPMENT=true; ./gradlew :libandroid-navigation:uploadArchives

dex-count:
	cd navigation; ./gradlew countDebugDexMethods
	cd navigation; ./gradlew countReleaseDexMethods

navigation-fixtures:
	# Navigation: Taylor street to Page street
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-122.413165,37.795042;-122.433378,37.7727?geometries=polyline6&overview=full&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o navigation/libandroid-navigation/src/test/res/navigation.json

	# Directions: polyline geometry with precision 5
	curl "https://api.mapbox.com/directions/v5/mapbox/driving/-122.416667,37.783333;-121.900000,37.333333?geometries=polyline&steps=true&access_token=$(MAPBOX_ACCESS_TOKEN)" \
		-o navigation/libandroid-navigation/src/test/res/directions_v5.json
