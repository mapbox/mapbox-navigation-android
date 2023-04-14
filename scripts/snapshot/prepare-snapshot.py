import subprocess
import sys

import requests

from utils import get_latest_tag, get_snapshot_branch, is_snapshot_week, get_dependency_version

if not is_snapshot_week():
    print('Navigation SDK snapshot must not be released today (rc or GA release was released this week).')
    sys.exit(1)

maps_version = get_dependency_version('https://api.github.com/repos/mapbox/mapbox-maps-android-internal/releases')
if not maps_version:
    print('Expected Maps release was not released.')
    sys.exit(1)
print('Bumping Maps to ' + maps_version)

nav_native_version = get_dependency_version('https://api.github.com/repos/mapbox/mapbox-navigation-native/releases')
if not nav_native_version:
    print('Expected Nav Native release was not released.')
    sys.exit(1)
print('Bumping Nav Native to ' + nav_native_version)

tags = requests.get('https://api.github.com/repos/mapbox/mapbox-navigation-android/git/refs/tags').json()
latest_tag = get_latest_tag(tags)
print('Latest no-patch release is ' + latest_tag)

snapshot_branch = get_snapshot_branch(latest_tag)
print('Snapshot branch is ' + snapshot_branch)
subprocess.run("git checkout " + snapshot_branch, shell=True, check=True)

versions_file_name = 'gradle/dependencies.gradle'
versions_lines = open(versions_file_name, 'r').readlines()
maps_version_line = ''
nav_native_version_line = ''
for line in versions_lines:
    if 'mapboxMapSdk' in line and maps_version_line == '':
        maps_version_line = line
    if 'mapboxNavigatorVersion = \'' in line:
        nav_native_version_line = line

versions_file = open(versions_file_name, 'r').read().replace(
    nav_native_version_line,
    '      mapboxNavigatorVersion = \'' + nav_native_version + '\'\n'
).replace(
    maps_version_line,
    '      mapboxMapSdk : \'' + maps_version + '\',\n'
)
open(versions_file_name, 'w').write(versions_file)
